export type ApiResponse<T> = {
  success: boolean;
  code: string;
  message: string;
  data: T;
};

export type KnowledgeBase = {
  id: string;
  name: string;
};

export type DocumentStatus =
  | 'PENDING'
  | 'PARSING'
  | 'CHUNKING'
  | 'EMBEDDING'
  | 'COMPLETED'
  | 'FAILED';

export type KnowledgeDocument = {
  id: string;
  originalFilename: string;
  status: DocumentStatus;
  errorMessage: string | null;
};

export type DocumentUploadResult = {
  document: {
    id: string;
    knowledgeBaseId: string;
    originalFilename: string;
    storagePath: string;
    status: DocumentStatus;
    errorMessage: string | null;
  };
};

export type DocumentReingestBatchResult = {
  knowledgeBaseId: string;
  totalCount: number;
  submittedCount: number;
  failedCount: number;
  documents: Array<unknown>;
  failures: Array<{
    documentId: string;
    reason: string;
  }>;
};

export type RagCitation = {
  documentId: string;
  chunkIndex: number;
  content: string;
};

export type RagGroundingEvaluation = {
  grounded: boolean;
  conclusion: string;
  unsupportedClaims: string[];
};

export type RagAnswerResponse = {
  knowledgeBaseId: string;
  question: string;
  answer: string;
  citations: RagCitation[];
  groundingEvaluation: RagGroundingEvaluation;
  usedModel: boolean;
  retrievalSummary: string;
};

export type AiToolDefinition = {
  name: string;
  description: string;
  inputSchema: string;
};

export type AgentChatStreamEvent = {
  type: 'session' | 'delta' | 'citations' | 'done' | 'error';
  sessionId: string;
  content: string;
  citations: RagCitation[];
};

export type ChatMessage = {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  citations?: RagCitation[];
};
