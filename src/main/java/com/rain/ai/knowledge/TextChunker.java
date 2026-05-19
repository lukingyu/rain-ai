package com.rain.ai.knowledge;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TextChunker {

    private static final int MAX_CHUNK_TOKEN_COUNT = 420;
    private static final int OVERLAP_TOKEN_COUNT = 80;
    private static final Pattern BLANK_LINE_PATTERN = Pattern.compile("\\n\\s*\\n+");
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6}\\s+.+|第[一二三四五六七八九十百千万0-9]+[章节篇].*|[一二三四五六七八九十]+[、.．].+|\\d+[、.．].+)$");
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[^。！？!?；;\\n]+[。！？!?；;]?");

    public List<ChunkSegment> split(String text) {
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return List.of();
        }

        List<TextBlock> blocks = new ArrayList<>();
        for (TextBlock block : parseBlocks(normalized)) {
            blocks.addAll(splitOversizedBlock(block));
        }
        return mergeBlocks(blocks);
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t]+\\n", "\n")
                .trim();
    }

    private List<TextBlock> parseBlocks(String text) {
        List<TextBlock> blocks = new ArrayList<>();
        Matcher matcher = BLANK_LINE_PATTERN.matcher(text);
        String currentSection = "正文";
        int start = 0;
        while (matcher.find()) {
            currentSection = addBlock(text, start, matcher.start(), currentSection, blocks);
            start = matcher.end();
        }
        addBlock(text, start, text.length(), currentSection, blocks);
        return blocks;
    }

    private String addBlock(
            String text,
            int rawStart,
            int rawEnd,
            String currentSection,
            List<TextBlock> blocks
    ) {
        int start = trimStart(text, rawStart, rawEnd);
        int end = trimEnd(text, start, rawEnd);
        if (start >= end) {
            return currentSection;
        }

        String content = text.substring(start, end);
        if (isHeading(content)) {
            return normalizeHeading(content);
        }
        blocks.add(new TextBlock(content, currentSection, start, end, "paragraph"));
        return currentSection;
    }

    private int trimStart(String text, int start, int end) {
        int index = start;
        while (index < end && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index;
    }

    private int trimEnd(String text, int start, int end) {
        int index = end;
        while (index > start && Character.isWhitespace(text.charAt(index - 1))) {
            index--;
        }
        return index;
    }

    private boolean isHeading(String content) {
        String singleLine = content.replace('\n', ' ').trim();
        return singleLine.length() <= 80 && HEADING_PATTERN.matcher(singleLine).matches();
    }

    private String normalizeHeading(String content) {
        return content.replaceFirst("^#{1,6}\\s+", "").trim();
    }

    private List<TextBlock> splitOversizedBlock(TextBlock block) {
        if (estimateTokenCount(block.content()) <= MAX_CHUNK_TOKEN_COUNT) {
            return List.of(block);
        }

        List<TextBlock> sentences = splitBySentence(block);
        List<TextBlock> result = new ArrayList<>();
        List<TextBlock> current = new ArrayList<>();
        int currentTokens = 0;
        for (TextBlock sentence : sentences) {
            int sentenceTokens = estimateTokenCount(sentence.content());
            if (!current.isEmpty() && currentTokens + sentenceTokens > MAX_CHUNK_TOKEN_COUNT) {
                result.add(combineBlocks(current, block.sectionTitle(), "sentence"));
                current.clear();
                currentTokens = 0;
            }
            current.add(sentence);
            currentTokens += sentenceTokens;
        }
        if (!current.isEmpty()) {
            result.add(combineBlocks(current, block.sectionTitle(), "sentence"));
        }
        return result;
    }

    private List<TextBlock> splitBySentence(TextBlock block) {
        List<TextBlock> sentences = new ArrayList<>();
        Matcher matcher = SENTENCE_PATTERN.matcher(block.content());
        while (matcher.find()) {
            String sentence = matcher.group().trim();
            if (sentence.isBlank()) {
                continue;
            }
            int start = block.charStart() + matcher.start();
            int end = block.charStart() + matcher.end();
            if (estimateTokenCount(sentence) <= MAX_CHUNK_TOKEN_COUNT) {
                sentences.add(new TextBlock(sentence, block.sectionTitle(), start, end, "sentence"));
            } else {
                sentences.addAll(splitLongSentence(sentence, block.sectionTitle(), start));
            }
        }
        return sentences;
    }

    private List<TextBlock> splitLongSentence(String sentence, String sectionTitle, int globalStart) {
        List<TextBlock> parts = new ArrayList<>();
        int start = 0;
        while (start < sentence.length()) {
            int end = Math.min(sentence.length(), start + MAX_CHUNK_TOKEN_COUNT);
            String part = sentence.substring(start, end).trim();
            if (!part.isBlank()) {
                parts.add(new TextBlock(
                        part,
                        sectionTitle,
                        globalStart + start,
                        globalStart + end,
                        "char"
                ));
            }
            start = end;
        }
        return parts;
    }

    private List<ChunkSegment> mergeBlocks(List<TextBlock> blocks) {
        List<ChunkSegment> segments = new ArrayList<>();
        List<TextBlock> current = new ArrayList<>();
        int currentTokens = 0;

        for (TextBlock block : blocks) {
            int blockTokens = estimateTokenCount(block.content());
            if (!current.isEmpty() && currentTokens + blockTokens > MAX_CHUNK_TOKEN_COUNT) {
                segments.add(toSegment(current));
                current = overlapTail(current);
                currentTokens = current.stream().mapToInt(value -> estimateTokenCount(value.content())).sum();
                if (currentTokens + blockTokens > MAX_CHUNK_TOKEN_COUNT) {
                    current.clear();
                    currentTokens = 0;
                }
            }
            current.add(block);
            currentTokens += blockTokens;
        }
        if (!current.isEmpty()) {
            segments.add(toSegment(current));
        }
        return segments;
    }

    private List<TextBlock> overlapTail(List<TextBlock> source) {
        List<TextBlock> tail = new ArrayList<>();
        int tokens = 0;
        for (int index = source.size() - 1; index >= 0; index--) {
            TextBlock block = source.get(index);
            int blockTokens = estimateTokenCount(block.content());
            if (tokens + blockTokens > OVERLAP_TOKEN_COUNT) {
                break;
            }
            tail.add(0, block);
            tokens += blockTokens;
        }
        return tail;
    }

    private TextBlock combineBlocks(List<TextBlock> blocks, String sectionTitle, String boundary) {
        StringBuilder content = new StringBuilder();
        for (TextBlock block : blocks) {
            if (!content.isEmpty()) {
                content.append('\n');
            }
            content.append(block.content());
        }
        return new TextBlock(
                content.toString(),
                sectionTitle,
                blocks.getFirst().charStart(),
                blocks.getLast().charEnd(),
                boundary
        );
    }

    private ChunkSegment toSegment(List<TextBlock> blocks) {
        String content = buildContent(blocks);
        return new ChunkSegment(
                content,
                estimateTokenCount(content),
                resolveSectionTitle(blocks),
                blocks.getFirst().charStart(),
                blocks.getLast().charEnd(),
                resolveBoundary(blocks),
                hash(content)
        );
    }

    private String buildContent(List<TextBlock> blocks) {
        StringBuilder builder = new StringBuilder();
        String lastSection = null;
        for (TextBlock block : blocks) {
            if (!Objects.equals(lastSection, block.sectionTitle())) {
                if (!builder.isEmpty()) {
                    builder.append('\n');
                }
                builder.append("【章节：").append(block.sectionTitle()).append("】\n");
                lastSection = block.sectionTitle();
            }
            if (!builder.isEmpty() && builder.charAt(builder.length() - 1) != '\n') {
                builder.append('\n');
            }
            builder.append(block.content()).append('\n');
        }
        return builder.toString().trim();
    }

    private String resolveSectionTitle(List<TextBlock> blocks) {
        String first = blocks.getFirst().sectionTitle();
        for (TextBlock block : blocks) {
            if (!Objects.equals(first, block.sectionTitle())) {
                return "多个章节";
            }
        }
        return first;
    }

    private String resolveBoundary(List<TextBlock> blocks) {
        boolean hasChar = blocks.stream().anyMatch(block -> "char".equals(block.boundary()));
        if (hasChar) {
            return "recursive-char";
        }
        boolean hasSentence = blocks.stream().anyMatch(block -> "sentence".equals(block.boundary()));
        return hasSentence ? "recursive-sentence" : "semantic-paragraph";
    }

    public int estimateTokenCount(String content) {
        int cjkCount = 0;
        int asciiCount = 0;
        for (int index = 0; index < content.length(); index++) {
            char value = content.charAt(index);
            if (isCjk(value)) {
                cjkCount++;
            } else if (!Character.isWhitespace(value)) {
                asciiCount++;
            }
        }
        return Math.max(1, cjkCount + Math.ceilDiv(asciiCount, 4));
    }

    private boolean isCjk(char value) {
        Character.UnicodeScript script = Character.UnicodeScript.of(value);
        return script == Character.UnicodeScript.HAN
                || script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA
                || script == Character.UnicodeScript.HANGUL;
    }

    private String hash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", exception);
        }
    }

    private record TextBlock(
            String content,
            String sectionTitle,
            int charStart,
            int charEnd,
            String boundary
    ) {
    }
}
