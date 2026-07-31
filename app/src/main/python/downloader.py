import os
import yt_dlp

def get_video_info(url):
    try:
        ydl_opts = {
            'quiet': True,
            'no_warnings': True,
            'nocheckcertificate': True,
        }
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=False)
            
            title = info.get('title', 'Видео')
            thumbnail = info.get('thumbnail', '')
            uploader = info.get('uploader', info.get('channel', ''))
            duration_sec = info.get('duration', 0)
            
            duration_str = ""
            if duration_sec:
                m, s = divmod(int(duration_sec), 60)
                h, m = divmod(m, 60)
                if h > 0:
                    duration_str = f"{h}:{m:02d}:{s:02d}"
                else:
                    duration_str = f"{m:02d}:{s:02d}"

            raw_formats = info.get('formats', [])
            formats_list = []
            
            seen_heights = set()
            
            for fmt in raw_formats:
                height = fmt.get('height')
                ext = fmt.get('ext', 'mp4')
                filesize = fmt.get('filesize') or fmt.get('filesize_approx') or 0
                
                if filesize:
                    size_mb = filesize / (1024 * 1024)
                    size_str = f"~{size_mb:.1f} МБ"
                else:
                    size_str = "Размер н/д"
                
                if height and height not in seen_heights and height in [1080, 720, 480, 360, 240, 144]:
                    seen_heights.add(height)
                    formats_list.append({
                        "format_id": f"best[height<={height}][ext=mp4]/best[height<={height}]",
                        "label": f"Видео {height}p ({ext.upper()})",
                        "ext": ext,
                        "size": size_str,
                        "height": height,
                        "is_audio": False
                    })
            
            # Sort heights descending
            formats_list.sort(key=lambda x: x.get('height', 0), reverse=True)
            
            # Insert best combined MP4 at top
            formats_list.insert(0, {
                "format_id": "best[ext=mp4]/best",
                "label": "Максимальное качество (MP4)",
                "ext": "mp4",
                "size": "Авто",
                "height": 9999,
                "is_audio": False
            })
            
            # Add audio option at bottom
            formats_list.append({
                "format_id": "bestaudio[ext=m4a]/bestaudio/best",
                "label": "Только аудио (M4A)",
                "ext": "m4a",
                "size": "Аудиодорожка",
                "height": 0,
                "is_audio": True
            })

            return {
                "success": True,
                "title": title,
                "thumbnail": thumbnail,
                "uploader": uploader,
                "duration": duration_str,
                "formats": formats_list,
                "error": ""
            }
    except Exception as e:
        return {
            "success": False,
            "title": "",
            "thumbnail": "",
            "uploader": "",
            "duration": "",
            "formats": [],
            "error": str(e)
        }

def download_video(url, download_path, filename, format_id="best[ext=mp4]/best", progress_listener=None):
    try:
        # Create output path if it doesn't exist
        if not os.path.exists(download_path):
            try:
                os.makedirs(download_path, exist_ok=True)
            except Exception:
                pass
                
        # Define output template for yt-dlp
        outtmpl = os.path.join(download_path, filename)
        
        def my_hook(d):
            if progress_listener is not None:
                try:
                    if progress_listener.isCancelled():
                        raise Exception("DOWNLOAD_CANCELLED")
                except Exception as e:
                    if "DOWNLOAD_CANCELLED" in str(e):
                        raise e

            if d.get('status') == 'downloading' and progress_listener is not None:
                total = d.get('total_bytes') or d.get('total_bytes_estimate') or 0
                downloaded = d.get('downloaded_bytes', 0)
                percent = float((downloaded / total) * 100.0) if total > 0 else 0.0
                speed = d.get('speed') or 0
                try:
                    progress_listener.onProgress(
                        int(downloaded),
                        int(total),
                        float(percent),
                        int(speed)
                    )
                except Exception:
                    pass

        ydl_opts = {
            'format': format_id,
            'outtmpl': outtmpl,
            'quiet': True,
            'no_warnings': True,
            'nocheckcertificate': True,
            'progress_hooks': [my_hook],
        }
        
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            # Download and extract info
            info = ydl.extract_info(url, download=True)
            title = info.get('title', 'Видео') if info else 'Видео'
            return {
                "success": True,
                "title": title,
                "error": ""
            }
    except Exception as e:
        err_str = str(e)
        if "DOWNLOAD_CANCELLED" in err_str:
            return {
                "success": False,
                "title": "",
                "error": "DOWNLOAD_CANCELLED"
            }
        return {
            "success": False,
            "title": "",
            "error": err_str
        }

