package com.ssginc8.docto.global.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "cloud.default.image")
public class ImageDefaultProperties {
	private String url; // url로 수정
}
