package com.bigproject.modules.learning.controller;

import com.bigproject.core.security.CurrentUser;
import com.bigproject.modules.learning.service.MediaService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/media/lessons")
public class MediaController {
  private final MediaService media;

  public MediaController(MediaService media) {
    this.media = media;
  }

  @GetMapping("/{lessonId}")
  public ResponseEntity<Resource> video(
      @PathVariable long lessonId, @AuthenticationPrincipal CurrentUser user) {
    return ResponseEntity.ok()
        .contentType(MediaType.valueOf("video/mp4"))
        .cacheControl(CacheControl.noStore())
        .body(media.video(lessonId, user));
  }
}
