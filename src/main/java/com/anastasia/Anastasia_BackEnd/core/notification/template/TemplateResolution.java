package com.anastasia.Anastasia_BackEnd.core.notification.template;

public record TemplateResolution(String identifier,
                                 String content,
                                 TemplateSource source) {

    public static TemplateResolution database(String identifier, String content) {
        return new TemplateResolution(identifier, content, TemplateSource.DATABASE);
    }

    public static TemplateResolution s3(String identifier, String content) {
        return new TemplateResolution(identifier, content, TemplateSource.S3);
    }

    public static TemplateResolution classpath(String identifier) {
        return new TemplateResolution(identifier, null, TemplateSource.CLASSPATH);
    }

    public boolean hasInlineContent() {
        return content != null;
    }
}

