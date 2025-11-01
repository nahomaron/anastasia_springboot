package com.anastasia.Anastasia_BackEnd.UnitTests.config;

import com.anastasia.Anastasia_BackEnd.common.config.S3Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThat;

class S3ConfigTest {

    private S3Config s3Config;

    @BeforeEach
    void setUp() {
        s3Config = new S3Config();
        ReflectionTestUtils.setField(s3Config, "awsAccessKey", "access");
        ReflectionTestUtils.setField(s3Config, "awsSecretKey", "secret");
        ReflectionTestUtils.setField(s3Config, "region", "us-east-1");
    }

    @Test
    void s3Client_shouldBuildClientWithProvidedRegion() {
        S3Client client = s3Config.s3Client();

        assertThat(client).isNotNull();
    }

    @Test
    void s3Presigner_shouldShareRegionConfiguration() {
        S3Presigner presigner = s3Config.s3Presigner();

        assertThat(presigner).isNotNull();
    }
}
