package org.example.springboot.ai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @Author: LZL
 * @CreateTime: 2026-05-12
 * @Description:
 * @Version: 1.0
 */
@SpringBootTest
public class Aicode {
    @Autowired
    private HeritageTools heritageTools;

    @Test
    public void test() {
        String sessionMessages = heritageTools.getSessionMessages("0c73f6b8-7820-4812-bbd5-47fdb54e3d86");
        System.out.println(sessionMessages);
    }

}
