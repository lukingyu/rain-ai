import {
  AlertTriangle,
  Bot,
  CheckCircle2,
  Circle,
  Database,
  FileText,
  Layers3,
  Loader2,
  MessageSquare,
  RefreshCw,
  RotateCcw,
  Search,
  Send,
  Server,
  Sparkles,
  Upload,
  Wrench,
  XCircle
} from 'lucide-react';
import { FormEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { api, streamAgentChat } from './api/client';
import type {
  AiToolDefinition,
  ChatMessage,
  DocumentReingestBatchResult,
  DocumentStatus,
  KnowledgeBase,
  KnowledgeDocument,
  RagAnswerResponse,
  RagCitation
} from './types';

const statusText: Record<DocumentStatus, string> = {
  PENDING: '等待',
  PARSING: '解析',
  CHUNKING: '切分',
  EMBEDDING: '向量化',
  COMPLETED: '完成',
  FAILED: '失败'
};

const statusIcon: Record<DocumentStatus, typeof Circle> = {
  PENDING: Circle,
  PARSING: Loader2,
  CHUNKING: Layers3,
  EMBEDDING: Database,
  COMPLETED: CheckCircle2,
  FAILED: XCircle
};

const createId = () => crypto.randomUUID();

const formatError = (error: unknown) => (error instanceof Error ? error.message : '请求失败');

export function App() {
  const [apiBaseUrl, setApiBaseUrl] = useState(() => localStorage.getItem('rain-ai-api-base') ?? '');
  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>([]);
  const [selectedKnowledgeBaseId, setSelectedKnowledgeBaseId] = useState('');
  const [documents, setDocuments] = useState<KnowledgeDocument[]>([]);
  const [tools, setTools] = useState<AiToolDefinition[]>([]);
  const [knowledgeBaseName, setKnowledgeBaseName] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [ragQuestion, setRagQuestion] = useState('');
  const [ragAnswer, setRagAnswer] = useState<RagAnswerResponse | null>(null);
  const [agentInput, setAgentInput] = useState('');
  const [agentSessionId, setAgentSessionId] = useState('');
  const [agentMessages, setAgentMessages] = useState<ChatMessage[]>([]);
  const [notice, setNotice] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [loadingKey, setLoadingKey] = useState('');
  const abortRef = useRef<AbortController | null>(null);

  const selectedKnowledgeBase = useMemo(
    () => knowledgeBases.find(item => item.id === selectedKnowledgeBaseId) ?? null,
    [knowledgeBases, selectedKnowledgeBaseId]
  );

  const documentSummary = useMemo(() => {
    return documents.reduce(
      (summary, document) => {
        summary.total += 1;
        summary[document.status] += 1;
        return summary;
      },
      {
        total: 0,
        PENDING: 0,
        PARSING: 0,
        CHUNKING: 0,
        EMBEDDING: 0,
        COMPLETED: 0,
        FAILED: 0
      } satisfies Record<DocumentStatus | 'total', number>
    );
  }, [documents]);

  const showError = useCallback((error: unknown) => {
    setErrorMessage(formatError(error));
  }, []);

  const refreshKnowledgeBases = useCallback(async () => {
    const list = await api.listKnowledgeBases(apiBaseUrl);
    setKnowledgeBases(list);
    setSelectedKnowledgeBaseId(current => current || list[0]?.id || '');
  }, [apiBaseUrl]);

  const refreshDocuments = useCallback(
    async (knowledgeBaseId = selectedKnowledgeBaseId) => {
      if (!knowledgeBaseId) {
        setDocuments([]);
        return;
      }
      setDocuments(await api.listDocuments(apiBaseUrl, knowledgeBaseId));
    },
    [apiBaseUrl, selectedKnowledgeBaseId]
  );

  const refreshTools = useCallback(async () => {
    setTools(await api.listTools(apiBaseUrl));
  }, [apiBaseUrl]);

  useEffect(() => {
    localStorage.setItem('rain-ai-api-base', apiBaseUrl);
  }, [apiBaseUrl]);

  useEffect(() => {
    setLoadingKey('initial');
    Promise.all([refreshKnowledgeBases(), refreshTools()])
      .catch(showError)
      .finally(() => setLoadingKey(''));
  }, [refreshKnowledgeBases, refreshTools, showError]);

  useEffect(() => {
    refreshDocuments().catch(showError);
  }, [refreshDocuments, showError]);

  async function runAction(actionKey: string, action: () => Promise<void>) {
    setLoadingKey(actionKey);
    setErrorMessage('');
    setNotice('');
    try {
      await action();
    } catch (error) {
      showError(error);
    } finally {
      setLoadingKey('');
    }
  }

  function createKnowledgeBase(event: FormEvent) {
    event.preventDefault();
    const name = knowledgeBaseName.trim();
    if (!name) {
      return;
    }
    runAction('create-kb', async () => {
      const knowledgeBase = await api.createKnowledgeBase(apiBaseUrl, name);
      setKnowledgeBaseName('');
      await refreshKnowledgeBases();
      setSelectedKnowledgeBaseId(knowledgeBase.id);
      setNotice(`已创建知识库：${knowledgeBase.name}`);
    });
  }

  function uploadDocument(event: FormEvent) {
    event.preventDefault();
    if (!selectedKnowledgeBaseId || !selectedFile) {
      return;
    }
    runAction('upload', async () => {
      await api.uploadDocument(apiBaseUrl, selectedKnowledgeBaseId, selectedFile);
      setSelectedFile(null);
      await refreshDocuments();
      setNotice('文档已登记，等待 RocketMQ 摄取');
    });
  }

  function reingestFailedDocuments() {
    if (!selectedKnowledgeBaseId) {
      return;
    }
    runAction('reingest-failed', async () => {
      const result = await api.reingestFailedDocuments(apiBaseUrl, selectedKnowledgeBaseId);
      await refreshDocuments();
      setNotice(formatBatchResult('失败文档重驱动', result));
    });
  }

  function reingestAllDocuments() {
    if (!selectedKnowledgeBaseId) {
      return;
    }
    runAction('reingest-all', async () => {
      const result = await api.reingestAllDocuments(apiBaseUrl, selectedKnowledgeBaseId);
      await refreshDocuments();
      setNotice(formatBatchResult('整库重摄取', result));
    });
  }

  function askRag(event: FormEvent) {
    event.preventDefault();
    const question = ragQuestion.trim();
    if (!selectedKnowledgeBaseId || !question) {
      return;
    }
    runAction('rag', async () => {
      setRagAnswer(await api.askRag(apiBaseUrl, selectedKnowledgeBaseId, question));
    });
  }

  function sendAgentMessage(event: FormEvent) {
    event.preventDefault();
    const message = agentInput.trim();
    if (!message || loadingKey === 'agent') {
      return;
    }

    const assistantId = createId();
    const userMessage: ChatMessage = { id: createId(), role: 'user', content: message };
    const assistantMessage: ChatMessage = { id: assistantId, role: 'assistant', content: '' };
    setAgentMessages(current => [...current, userMessage, assistantMessage]);
    setAgentInput('');
    setLoadingKey('agent');
    setErrorMessage('');
    setNotice('');

    const controller = new AbortController();
    abortRef.current = controller;
    streamAgentChat(
      apiBaseUrl,
      {
        sessionId: agentSessionId || undefined,
        knowledgeBaseId: selectedKnowledgeBaseId || undefined,
        message
      },
      {
        signal: controller.signal,
        onEvent: eventData => {
          if (eventData.type === 'session') {
            setAgentSessionId(eventData.sessionId);
            return;
          }
          if (eventData.type === 'delta') {
            setAgentMessages(current =>
              current.map(item =>
                item.id === assistantId ? { ...item, content: item.content + eventData.content } : item
              )
            );
            return;
          }
          if (eventData.type === 'citations') {
            setAgentMessages(current =>
              current.map(item =>
                item.id === assistantId ? { ...item, citations: eventData.citations } : item
              )
            );
            return;
          }
          if (eventData.type === 'error') {
            setAgentMessages(current =>
              current.map(item =>
                item.id === assistantId ? { ...item, content: eventData.content || '模型调用失败' } : item
              )
            );
          }
        }
      }
    )
      .catch(error => {
        if (!controller.signal.aborted) {
          showError(error);
          setAgentMessages(current =>
            current.map(item =>
              item.id === assistantId && !item.content
                ? { ...item, content: '流式请求失败，请检查后端服务和模型配置。' }
                : item
            )
          );
        }
      })
      .finally(() => {
        if (abortRef.current === controller) {
          abortRef.current = null;
        }
        setLoadingKey('');
      });
  }

  function newAgentSession() {
    abortRef.current?.abort();
    abortRef.current = null;
    setAgentSessionId('');
    setAgentMessages([]);
  }

  const isBusy = Boolean(loadingKey);

  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Rain AI Agent Platform</p>
          <h1>企业知识库 Agent 工作台</h1>
        </div>
        <div className="api-target">
          <Server size={18} />
          <input
            value={apiBaseUrl}
            onChange={event => setApiBaseUrl(event.target.value)}
            placeholder="/api"
            aria-label="后端地址"
          />
          <button className="icon-button" title="刷新基础数据" onClick={() => runAction('initial', async () => {
            await Promise.all([refreshKnowledgeBases(), refreshTools()]);
            await refreshDocuments();
          })}>
            <RefreshCw size={18} className={loadingKey === 'initial' ? 'spinning' : ''} />
          </button>
        </div>
      </header>

      {(notice || errorMessage) && (
        <section className={`banner ${errorMessage ? 'banner-error' : 'banner-success'}`}>
          {errorMessage ? <AlertTriangle size={18} /> : <CheckCircle2 size={18} />}
          <span>{errorMessage || notice}</span>
        </section>
      )}

      <section className="workspace-grid">
        <aside className="panel knowledge-panel">
          <PanelTitle icon={Database} title="知识库" action={
            <button className="icon-button" title="刷新知识库" onClick={() => runAction('refresh-kb', refreshKnowledgeBases)}>
              <RefreshCw size={17} className={loadingKey === 'refresh-kb' ? 'spinning' : ''} />
            </button>
          } />

          <form className="inline-form" onSubmit={createKnowledgeBase}>
            <input
              value={knowledgeBaseName}
              onChange={event => setKnowledgeBaseName(event.target.value)}
              placeholder="新知识库名称"
              maxLength={120}
            />
            <button disabled={loadingKey === 'create-kb'} type="submit">
              <Sparkles size={16} />
              创建
            </button>
          </form>

          <div className="knowledge-list">
            {knowledgeBases.map(item => (
              <button
                key={item.id}
                className={`knowledge-item ${item.id === selectedKnowledgeBaseId ? 'active' : ''}`}
                onClick={() => setSelectedKnowledgeBaseId(item.id)}
              >
                <Database size={17} />
                <span>{item.name}</span>
              </button>
            ))}
            {!knowledgeBases.length && <EmptyState text="暂无知识库" />}
          </div>

          <div className="tool-box">
            <div className="tool-box-title">
              <Wrench size={17} />
              <span>Spring AI Tools</span>
            </div>
            {tools.slice(0, 6).map(tool => (
              <div className="tool-row" key={tool.name} title={tool.description}>
                <span>{tool.name}</span>
                <small>{tool.description}</small>
              </div>
            ))}
            {!tools.length && <EmptyState text="暂无工具定义" />}
          </div>
        </aside>

        <section className="panel document-panel">
          <PanelTitle icon={FileText} title="文档摄取" action={
            <button className="icon-button" title="刷新文档状态" onClick={() => runAction('refresh-docs', async () => refreshDocuments())}>
              <RefreshCw size={17} className={loadingKey === 'refresh-docs' ? 'spinning' : ''} />
            </button>
          } />

          <div className="selected-kb">
            <span>{selectedKnowledgeBase?.name ?? '未选择知识库'}</span>
            <small>{selectedKnowledgeBaseId || '创建或选择一个知识库'}</small>
          </div>

          <form className="upload-zone" onSubmit={uploadDocument}>
            <label className="file-picker">
              <Upload size={20} />
              <span>{selectedFile ? selectedFile.name : '选择文档'}</span>
              <input type="file" onChange={event => setSelectedFile(event.target.files?.[0] ?? null)} />
            </label>
            <button type="submit" disabled={!selectedKnowledgeBaseId || !selectedFile || loadingKey === 'upload'}>
              <Upload size={16} />
              上传
            </button>
          </form>

          <div className="status-strip">
            <Metric label="全部" value={documentSummary.total} />
            <Metric label="完成" value={documentSummary.COMPLETED} tone="green" />
            <Metric label="处理中" value={documentSummary.PARSING + documentSummary.CHUNKING + documentSummary.EMBEDDING} tone="blue" />
            <Metric label="失败" value={documentSummary.FAILED} tone="red" />
          </div>

          <div className="action-row">
            <button onClick={reingestFailedDocuments} disabled={!selectedKnowledgeBaseId || isBusy}>
              <RotateCcw size={16} />
              重驱动失败
            </button>
            <button className="secondary-button" onClick={reingestAllDocuments} disabled={!selectedKnowledgeBaseId || isBusy}>
              <RefreshCw size={16} />
              整库重摄取
            </button>
          </div>

          <div className="document-list">
            {documents.map(document => (
              <DocumentRow key={document.id} document={document} />
            ))}
            {!documents.length && <EmptyState text="暂无文档" />}
          </div>
        </section>

        <section className="panel rag-panel">
          <PanelTitle icon={Search} title="RAG 问答" />
          <form className="question-form" onSubmit={askRag}>
            <textarea
              value={ragQuestion}
              onChange={event => setRagQuestion(event.target.value)}
              placeholder="向当前知识库提问"
              maxLength={1000}
            />
            <button type="submit" disabled={!selectedKnowledgeBaseId || !ragQuestion.trim() || loadingKey === 'rag'}>
              <Send size={16} />
              提问
            </button>
          </form>

          <AnswerBlock answer={ragAnswer} loading={loadingKey === 'rag'} />
        </section>

        <section className="panel agent-panel">
          <PanelTitle icon={Bot} title="Agent Chat" action={
            <button className="icon-button" title="新会话" onClick={newAgentSession}>
              <MessageSquare size={17} />
            </button>
          } />
          <div className="session-line">
            <span>Session</span>
            <code>{agentSessionId || '未开始'}</code>
          </div>

          <div className="chat-window">
            {agentMessages.map(message => (
              <ChatBubble key={message.id} message={message} />
            ))}
            {!agentMessages.length && <EmptyState text="可以直接让 Agent 查询知识库、检索内容或重驱动文档" />}
          </div>

          <form className="agent-input" onSubmit={sendAgentMessage}>
            <textarea
              value={agentInput}
              onChange={event => setAgentInput(event.target.value)}
              placeholder="发送给 Agent"
              maxLength={1000}
            />
            <button type="submit" disabled={!agentInput.trim() || loadingKey === 'agent'}>
              <Send size={16} />
              发送
            </button>
          </form>
        </section>
      </section>
    </main>
  );
}

function PanelTitle({
  icon: Icon,
  title,
  action
}: {
  icon: typeof Database;
  title: string;
  action?: React.ReactNode;
}) {
  return (
    <div className="panel-title">
      <div>
        <Icon size={19} />
        <h2>{title}</h2>
      </div>
      {action}
    </div>
  );
}

function Metric({ label, value, tone = 'neutral' }: { label: string; value: number; tone?: 'neutral' | 'green' | 'blue' | 'red' }) {
  return (
    <div className={`metric metric-${tone}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function DocumentRow({ document }: { document: KnowledgeDocument }) {
  const Icon = statusIcon[document.status];
  return (
    <article className="document-row">
      <div className="document-main">
        <FileText size={18} />
        <div>
          <strong>{document.originalFilename}</strong>
          <small>{document.id}</small>
        </div>
      </div>
      <div className={`status-badge status-${document.status.toLowerCase()}`}>
        <Icon size={15} className={['PARSING', 'EMBEDDING'].includes(document.status) ? 'spinning' : ''} />
        <span>{statusText[document.status]}</span>
      </div>
      {document.errorMessage && <p className="document-error">{document.errorMessage}</p>}
    </article>
  );
}

function AnswerBlock({ answer, loading }: { answer: RagAnswerResponse | null; loading: boolean }) {
  if (loading) {
    return <EmptyState text="RAG 正在召回知识库并生成回答" />;
  }
  if (!answer) {
    return <EmptyState text="回答会显示在这里" />;
  }

  return (
    <div className="answer-block">
      <p>{answer.answer}</p>
      <div className={`grounding ${answer.groundingEvaluation?.grounded ? 'grounded' : 'ungrounded'}`}>
        {answer.groundingEvaluation?.grounded ? <CheckCircle2 size={16} /> : <AlertTriangle size={16} />}
        <span>{answer.groundingEvaluation?.conclusion || '未返回依据性判断'}</span>
      </div>
      <CitationList citations={answer.citations} />
    </div>
  );
}

function ChatBubble({ message }: { message: ChatMessage }) {
  return (
    <article className={`chat-bubble ${message.role}`}>
      <strong>{message.role === 'user' ? '你' : 'Agent'}</strong>
      <p>{message.content || '...'}</p>
      {message.citations && message.citations.length > 0 && <CitationList citations={message.citations} compact />}
    </article>
  );
}

function CitationList({ citations, compact = false }: { citations: RagCitation[]; compact?: boolean }) {
  if (!citations?.length) {
    return null;
  }
  return (
    <div className={`citations ${compact ? 'compact' : ''}`}>
      {citations.map((citation, index) => (
        <details key={`${citation.documentId}-${citation.chunkIndex}-${index}`}>
          <summary>
            片段 {citation.chunkIndex + 1}
            <code>{citation.documentId}</code>
          </summary>
          <p>{citation.content}</p>
        </details>
      ))}
    </div>
  );
}

function EmptyState({ text }: { text: string }) {
  return <div className="empty-state">{text}</div>;
}

function formatBatchResult(prefix: string, result: DocumentReingestBatchResult) {
  return `${prefix}：共 ${result.totalCount} 个，已提交 ${result.submittedCount} 个，失败 ${result.failedCount} 个`;
}
