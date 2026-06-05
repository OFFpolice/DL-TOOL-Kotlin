import os
import yt_dlp

def download_video(url, download_path, filename):
    try:
        # Create output path if it doesn't exist
        if not os.path.exists(download_path):
            try:
                os.makedirs(download_path, exist_ok=True)
            except Exception:
                pass
                
        # Define output template for yt-dlp
        outtmpl = os.path.join(download_path, filename)
        
        # We use format 'best[ext=mp4]/best' to download a pre-merged audio+video stream (e.g. mp4).
        # This prevents yt-dlp from attempting to merge separate video and audio formats which requires ffmpeg.
        ydl_opts = {
            'format': 'best[ext=mp4]/best',
            'outtmpl': outtmpl,
            'quiet': True,
            'no_warnings': True,
            'nocheckcertificate': True,
        }
        
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            # Download and extract info
            info = ydl.extract_info(url, download=True)
            title = info.get('title', 'Видео')
            return {
                "success": True,
                "title": title,
                "error": ""
            }
    except Exception as e:
        return {
            "success": False,
            "title": "",
            "error": str(e)
        }
