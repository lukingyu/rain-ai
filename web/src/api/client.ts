import type {
  AgentChatStreamEvent,
  AiToolDefinition,
  ApiResponse,
  DocumentReingestBatchResult,
  DocumentUploadResult,
  KnowledgeBase,
  KnowledgeDocument,
  RagAnswerResponse
} from '../types';

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly code?: string,
    public readonly status?: number
  ) {
    super(message);
  }
}

const normalizeBaseUrl = (baseUrl: string) => baseUrl.trim().replace(/\/$/, '');

const buildUrl = (baseUrl: string, path: string) => `${normalizeBaseUrl(baseUrl)}${path}`;

async function request<T>(baseUrl: string, path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(buildUrl(baseUrl, path), init);
  const contentType = response.headers.get('content-type') ?? '';
  const body = contentType.includes('application/json') ? await response.json() : await response.text();

  if (!response.ok) {
    const message = typeof body === 'string' ? body : body.message;
    throw new ApiError(message || `请求失败：${response.status}`, undefined, response.status);
  }

  const apiResponse = body as ApiResponse<T>;
  if (!apiResponse.success) {
    throw new ApiError(apiResponse.message, apiResponse.code, response.status);
  }
  return apiResponse.data;
}

export const api = {
  listKnowledgeBases(baseUrl: string) {
    return request<KnowledgeBase[]>(baseUrl, '/api/knowledge-bases');
  },

  createKnowledgeBase(baseUrl: string, name: string) {
    return request<KnowledgeBase>(baseUrl, '/api/knowledge-bases', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name })
    });
  },

  listDocuments(baseUrl: string, knowledgeBaseId: string) {
    return request<KnowledgeDocument[]>(baseUrl, `/api/knowledge-bases/${knowledgeBaseId}/documents`);
  },

  uploadDocument(baseUrl: string, knowledgeBaseId: string, file: File) {
    const formData = new FormData();
    formData.append('file', file);
    return request<DocumentUploadResult>(baseUrl, `/api/knowledge-bases/${knowledgeBaseId}/documents`, {
      method: 'POST',
      body: formData
    });
  },

  reingestFailedDocuments(baseUrl: string, knowledgeBaseId: string) {
    return request<DocumentReingestBatchResult>(
      baseUrl,
      `/api/knowledge-bases/${knowledgeBaseId}/documents/failed/reingest`,
      { method: 'POST' }
    );
  },

  reingestAllDocuments(baseUrl: string, knowledgeBaseId: string) {
    return request<DocumentReingestBatchResult>(
      baseUrl,
      `/api/knowledge-bases/${knowledgeBaseId}/documents/reingest`,
      { method: 'POST' }
    );
  },

  askRag(baseUrl: string, knowledgeBaseId: string, question: string) {
    return request<RagAnswerResponse>(baseUrl, '/api/rag/ask', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ knowledgeBaseId, question })
    });
  },

  listTools(baseUrl: string) {
    return request<AiToolDefinition[]>(baseUrl, '/api/tools');
  }
};

export async function streamAgentChat(
  baseUrl: string,
  payload: {
    sessionId?: string;
    knowledgeBaseId?: string;
    message: string;
  },
  handlers: {
    onEvent: (event: AgentChatStreamEvent) => void;
    signal?: AbortSignal;
  }
) {
  const response = await fetch(buildUrl(baseUrl, '/api/agent/chat/stream'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
    signal: handlers.signal
  });

  if (!response.ok || !response.body) {
    throw new ApiError(`流式请求失败：${response.status}`, undefined, response.status);
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';

  while (true) {
    const { value, done } = await reader.read();
    if (done) {
      break;
    }
    buffer += decoder.decode(value, { stream: true });
    const frames = buffer.split(/\r?\n\r?\n/);
    buffer = frames.pop() ?? '';
    frames.forEach(frame => emitFrame(frame, handlers.onEvent));
  }

  if (buffer.trim()) {
    emitFrame(buffer, handlers.onEvent);
  }
}

function emitFrame(frame: string, onEvent: (event: AgentChatStreamEvent) => void) {
  const data = frame
    .split(/\r?\n/)
    .filter(line => line.startsWith('data:'))
    .map(line => line.slice(5).trimStart())
    .join('\n');

  if (!data) {
    return;
  }

  onEvent(JSON.parse(data) as AgentChatStreamEvent);
}
