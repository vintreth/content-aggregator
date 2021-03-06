package ru.skogmark.aggregator.parser.vk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.skogmark.aggregator.channel.Source;
import ru.skogmark.aggregator.core.PostImage;
import ru.skogmark.aggregator.core.PostImageSize;
import ru.skogmark.aggregator.core.content.Content;
import ru.skogmark.aggregator.core.content.ContentPost;
import ru.skogmark.aggregator.core.content.Parser;
import ru.skogmark.aggregator.core.content.ParsingContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Service
public class VkApiParser implements Parser {
    private static final Logger log = LoggerFactory.getLogger(VkApiParser.class);

    private final VkApiClient vkApiClient;

    public VkApiParser(@Nonnull VkApiClient vkApiClient) {
        this.vkApiClient = requireNonNull(vkApiClient, "vkApiClient");
    }

    @Nonnull
    @Override
    public Optional<Content> parse(@Nonnull ParsingContext parsingContext) {
        requireNonNull(parsingContext, "parsingContext");
        log.info("Parsing content in vk: limit={}, offset={}",
                parsingContext.getLimit(), parsingContext.getOffset().orElse(null));
        Source source = Source.getById(parsingContext.getSourceId());
        VkApiResult result = vkApiClient.getWall(GetWallRequest.builder()
                .setOwner(toOwner(source))
                .setCount(parsingContext.getLimit())
                .setOffset(parsingContext.getOffset().orElse(null))
                .build());
        if (result.isError() || result.getResponse().isEmpty()) {
            log.error("Vk api returned error result: result={}", result);
            return Optional.empty();
        }
        return Optional.of(new Content(
                result.getResponse().get().getItems().stream()
                        .map(VkApiParser::toPost)
                        .collect(Collectors.toList()),
                calculateNextOffset(
                        parsingContext.getOffset().orElse(null),
                        parsingContext.getLimit(),
                        result.getResponse().get().getCount())));
    }

    private static Owner toOwner(Source source) {
        switch (source) {
            case VK_LEPRA:
                return Owner.LEPRA;
            default:
                throw new IllegalArgumentException("Unknown source: source=" + source);
        }
    }

    private static ContentPost toPost(Item item) {
        ContentPost.Builder builder = ContentPost.builder()
                .setExternalId(item.getId())
                .setText(item.getText().orElse(null));
        if (!item.getAttachments().isEmpty()) {
            List<PostImage> images = item.getAttachments().stream()
                    .filter(attachment -> "photo".equals(attachment.getType()))
                    .filter(attachment -> attachment.getPhoto().isPresent())
                    .map(attachment -> new PostImage(attachment.getPhoto().get().getSizes().stream()
                            .map(size -> PostImageSize.builder()
                                    .setUuid(UUID.randomUUID().toString())
                                    .setSrc(size.getUrl())
                                    .setWidth(size.getWidth())
                                    .setHeight(size.getHeight())
                                    .build())
                            .collect(Collectors.toList())))
                    .collect(Collectors.toList());
            builder.setImages(images);
        }
        return builder.build();
    }

    private static long calculateNextOffset(@Nullable Long currentOffset,
                                            int limit,
                                            int totalMessagesCount) {
        if (currentOffset == null) {
            return totalMessagesCount - limit;
        } else if (currentOffset <= 0) {
            return 0;
        } else if (currentOffset - limit <= 0) {
            return 0;
        } else {
            return currentOffset - limit;
        }
    }
}
