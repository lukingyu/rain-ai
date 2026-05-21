package com.rain.ai.api;

import com.rain.ai.agent.AgentChatRequest;
import com.rain.ai.agent.AgentChatResponse;
import com.rain.ai.agent.AgentChatService;
import com.rain.ai.agent.AgentChatStream;
import com.rain.ai.agent.AgentChatStreamEvent;
import com.rain.ai.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentChatService agentChatService;

    public AgentController(AgentChatService agentChatService) {
        this.agentChatService = agentChatService;
    }

    @PostMapping("/chat")
    public ApiResponse<AgentChatResponse> chat(@Valid @RequestBody AgentChatRequest request) {
        return ApiResponse.success(agentChatService.chat(request));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody AgentChatRequest request) {
        AgentChatStream chatStream = agentChatService.stream(request);
        SseEmitter emitter = new SseEmitter(0L);
        AtomicReference<Disposable> disposableRef = new AtomicReference<>();

        if (!send(emitter, AgentChatStreamEvent.session(chatStream.sessionId()))) {
            return emitter;
        }

        Disposable disposable = chatStream.events()
                .subscribe(
                        event -> send(emitter, event),
                        error -> completeWithError(emitter, chatStream.sessionId(), error),
                        emitter::complete
                );
        disposableRef.set(disposable);
        emitter.onCompletion(() -> dispose(disposableRef));
        emitter.onTimeout(() -> dispose(disposableRef));
        emitter.onError(error -> dispose(disposableRef));
        return emitter;
    }

    private boolean send(SseEmitter emitter, AgentChatStreamEvent event) {
        try {
            emitter.send(SseEmitter.event().name(event.type()).data(event));
            return true;
        } catch (IOException exception) {
            emitter.completeWithError(exception);
            return false;
        }
    }

    private void completeWithError(SseEmitter emitter, String sessionId, Throwable error) {
        send(emitter, AgentChatStreamEvent.error(sessionId, error.getMessage()));
        emitter.completeWithError(error);
    }

    private void dispose(AtomicReference<Disposable> disposableRef) {
        Disposable disposable = disposableRef.get();
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }
}
