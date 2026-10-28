-- Route sample playback through access-checked, local media endpoints.
UPDATE lesson SET video_url = CONCAT('/api/media/lessons/', id);
