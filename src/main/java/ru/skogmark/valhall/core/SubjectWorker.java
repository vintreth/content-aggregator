package ru.skogmark.valhall.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.skogmark.valhall.core.content.ContentService;
import ru.skogmark.valhall.core.premoderation.PremoderationQueueService;
import ru.skogmark.valhall.core.premoderation.UnmoderatedPost;
import ru.skogmark.valhall.image.Image;
import ru.skogmark.valhall.image.ImageDownloadService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public class SubjectWorker {
    private static final Logger log = LoggerFactory.getLogger(SubjectWorker.class);

    private final ScheduledExecutorService subjectWorkerExecutor;
    private final ContentService contentService;
    private final PremoderationQueueService premoderationQueueService;
    private final ImageDownloadService imageDownloadService;

    public SubjectWorker(@Nonnull ScheduledExecutorService subjectWorkerExecutor,
                         @Nonnull ContentService contentService,
                         @Nonnull PremoderationQueueService premoderationQueueService,
                         @Nonnull ImageDownloadService imageDownloadService) {
        this.subjectWorkerExecutor = requireNonNull(subjectWorkerExecutor, "subjectWorkerExecutor");
        this.contentService = requireNonNull(contentService, "contentService");
        this.premoderationQueueService = requireNonNull(premoderationQueueService, "premoderationQueueService");
        this.imageDownloadService = requireNonNull(imageDownloadService, "imageDownloadService");
    }

    @PostConstruct
    public void start() {
        log.info("Starting worker");
        subjectWorkerExecutor.scheduleWithFixedDelay(this::checkTimetableAndExecuteWorker, 0, 1, TimeUnit.SECONDS);
    }

    public void checkTimetableAndExecuteWorker() {

    }

    public void doWork(SubjectContext subjectContext) {
        log.info("Starting to obtain contents and enqueueing the posts: workerName={}", subjectContext.getName());
        subjectContext.getSources().forEach(source ->
                contentService.getContent(source, content -> {
                    Optional<Image> image = imageDownloadService.downloadAndSave(content.getImageUri());
                    premoderationQueueService.enqueuePost(buildUnmoderatedPost(content.getText(),
                            image.flatMap(Image::getId).orElse(null)));
                }));
    }

    private static UnmoderatedPost buildUnmoderatedPost(@Nonnull String text, @Nullable Long imageId) {
        requireNonNull(text, "text");
        return UnmoderatedPost.builder()
                .text(text)
                .imageId(imageId)
                .build();
    }
}
