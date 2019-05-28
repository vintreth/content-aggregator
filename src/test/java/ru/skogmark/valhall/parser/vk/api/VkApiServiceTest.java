package ru.skogmark.valhall.parser.vk.api;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.skogmark.valhall.ApplicationContextAwareTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class VkApiServiceTest extends ApplicationContextAwareTest {
    @Autowired
    private VkApiService vkApiService;

    @Test
    public void name() throws InterruptedException {
        vkApiService.getUserSubscriptions(response -> {
            try {
                String content = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
                System.out.println(">>>>>>>>>>>>>>>" + content);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        Thread.sleep(5000);
    }
}