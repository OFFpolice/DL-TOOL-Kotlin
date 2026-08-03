import os
import sys
import json
import re
import urllib.request
import yt_dlp

def parse_version_tuple(ver_str):
    if not ver_str:
        return ()
    nums = re.findall(r'\d+', str(ver_str))
    return tuple(int(n) for n in nums)

def check_ytdlp_version():
    cur_ver = str(getattr(yt_dlp.version, '__version__', 'Неизвестно'))
    cur_tuple = parse_version_tuple(cur_ver)
    
    latest_ver = cur_ver
    error_msg = ""
    
    # Primary check via PyPI
    try:
        req = urllib.request.Request(
            "https://pypi.org/pypi/yt-dlp/json",
            headers={"User-Agent": "DL-TOOL-AndroidApp/1.0"}
        )
        with urllib.request.urlopen(req, timeout=8) as response:
            data = json.loads(response.read().decode('utf-8'))
            latest_ver = str(data.get("info", {}).get("version", cur_ver))
    except Exception as e1:
        # Fallback check via GitHub Releases API
        try:
            req_gh = urllib.request.Request(
                "https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest",
                headers={"User-Agent": "DL-TOOL-AndroidApp/1.0", "Accept": "application/vnd.github.v3+json"}
            )
            with urllib.request.urlopen(req_gh, timeout=8) as resp_gh:
                data_gh = json.loads(resp_gh.read().decode('utf-8'))
                tag_name = data_gh.get("tag_name", "")
                if tag_name:
                    latest_ver = tag_name.lstrip('v')
        except Exception as e2:
            error_msg = f"PyPI: {str(e1)}, GitHub: {str(e2)}"

    latest_tuple = parse_version_tuple(latest_ver)
    has_update = bool(latest_tuple and cur_tuple and latest_tuple > cur_tuple)

    return {
        "success": bool(latest_ver and not (error_msg and latest_ver == cur_ver)),
        "current_version": cur_ver,
        "latest_version": latest_ver,
        "has_update": has_update,
        "error": error_msg
    }

def update_ytdlp_package():
    try:
        import importlib
        import zipfile
        import shutil

        target_dir = os.path.dirname(os.path.dirname(os.path.abspath(yt_dlp.__file__)))
        pip_success = False

        # Method 1: Try pip internal CLI
        try:
            from pip._internal.cli.main import main as pip_main
            code = pip_main(['install', '--upgrade', '--no-deps', '--target', target_dir, 'yt-dlp'])
            if code == 0:
                pip_success = True
        except Exception:
            pip_success = False

        # Method 2: Direct Wheel download & extract from PyPI if pip failed
        if not pip_success:
            req = urllib.request.Request(
                "https://pypi.org/pypi/yt-dlp/json",
                headers={"User-Agent": "DL-TOOL-AndroidApp/1.0"}
            )
            with urllib.request.urlopen(req, timeout=15) as response:
                data = json.loads(response.read().decode('utf-8'))
                urls = data.get("urls", [])
                whl_url = None
                for u in urls:
                    if u.get("filename", "").endswith(".whl"):
                        whl_url = u.get("url")
                        break
                if not whl_url and urls:
                    whl_url = urls[0].get("url")

                if not whl_url:
                    return {
                        "success": False,
                        "new_version": "",
                        "error": "Ссылка на пакет обновления не найдена"
                    }

                whl_req = urllib.request.Request(whl_url, headers={"User-Agent": "DL-TOOL-AndroidApp/1.0"})
                temp_whl = os.path.join(target_dir, "temp_ytdlp.whl")
                with urllib.request.urlopen(whl_req, timeout=30) as whl_resp, open(temp_whl, "wb") as f_out:
                    shutil.copyfileobj(whl_resp, f_out)

                with zipfile.ZipFile(temp_whl, 'r') as zip_ref:
                    for member in zip_ref.namelist():
                        if member.startswith("yt_dlp/"):
                            zip_ref.extract(member, target_dir)

                if os.path.exists(temp_whl):
                    os.remove(temp_whl)

        importlib.invalidate_caches()
        importlib.reload(yt_dlp)
        new_ver = str(getattr(yt_dlp.version, '__version__', 'Неизвестно'))
        return {
            "success": True,
            "new_version": new_ver,
            "error": ""
        }
    except Exception as e:
        return {
            "success": False,
            "new_version": "",
            "error": str(e)
        }

def format_bytes_str(num_bytes):
    if not num_bytes or num_bytes <= 0:
        return ""
    if num_bytes < 1024 * 1024:
        return f"~{num_bytes / 1024:.0f} КБ"
    elif num_bytes < 1024 * 1024 * 1024:
        return f"~{num_bytes / (1024 * 1024):.1f} МБ"
    else:
        return f"~{num_bytes / (1024 * 1024 * 1024):.2f} ГБ"

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
            
            # Determine best audio stream size for adding to video-only DASH formats
            best_audio_bytes = 0
            for fmt in raw_formats:
                vcodec = fmt.get('vcodec', '')
                acodec = fmt.get('acodec', '')
                if (vcodec == 'none' or not vcodec) and acodec != 'none':
                    fs = fmt.get('filesize') or fmt.get('filesize_approx') or 0
                    if not fs and duration_sec and fmt.get('abr'):
                        fs = (fmt.get('abr') * 1000 / 8) * duration_sec
                    if fs > best_audio_bytes:
                        best_audio_bytes = fs
            
            if not best_audio_bytes and duration_sec:
                best_audio_bytes = (128 * 1000 / 8) * duration_sec

            seen_heights = set()
            max_calculated_bytes = info.get('filesize') or info.get('filesize_approx') or 0
            
            for fmt in raw_formats:
                height = fmt.get('height')
                ext = fmt.get('ext', 'mp4')
                filesize = fmt.get('filesize') or fmt.get('filesize_approx') or 0
                
                if not filesize and duration_sec:
                    tbr = fmt.get('tbr') or fmt.get('vbr') or 0
                    if tbr:
                        filesize = (tbr * 1000 / 8) * duration_sec
                
                is_video_only = (fmt.get('acodec') == 'none')
                total_bytes = filesize + (best_audio_bytes if is_video_only and filesize > 0 else 0)

                if total_bytes > max_calculated_bytes:
                    max_calculated_bytes = total_bytes

                size_str = format_bytes_str(total_bytes)
                if not size_str:
                    size_str = "~МБ"
                
                if height and height not in seen_heights and height in [2160, 1440, 1080, 720, 480, 360, 240, 144]:
                    seen_heights.add(height)
                    label_text = f"{height}p"
                    if height == 2160:
                        label_text = "4K Ultra HD"
                    elif height == 1440:
                        label_text = "1440p QHD"
                    elif height == 1080:
                        label_text = "1080p Full HD"
                    elif height == 720:
                        label_text = "720p HD"
                    elif height == 480:
                        label_text = "480p SD"
                    elif height == 240:
                        label_text = "240p SD"
                    
                    formats_list.append({
                        "format_id": f"best[height<={height}][ext=mp4]/best[height<={height}]",
                        "label": label_text,
                        "ext": ext,
                        "size": size_str,
                        "height": height,
                        "is_audio": False
                    })
            
            # Sort heights descending
            formats_list.sort(key=lambda x: x.get('height', 0), reverse=True)
            
            # Insert best combined MP4 at top
            top_size_str = format_bytes_str(max_calculated_bytes) if max_calculated_bytes > 0 else "best"
            formats_list.insert(0, {
                "format_id": "best[ext=mp4]/best",
                "label": "Авто",
                "ext": "mp4",
                "size": top_size_str,
                "height": 9999,
                "is_audio": False
            })
            
            # Add audio option at bottom
            audio_size_str = format_bytes_str(best_audio_bytes) if best_audio_bytes > 0 else "Аудио"
            formats_list.append({
                "format_id": "bestaudio[ext=m4a]/bestaudio/best",
                "label": "Аудио",
                "ext": "m4a",
                "size": audio_size_str,
                "height": 0,
                "is_audio": True
            })

            # Add thumbnail option at bottom
            if thumbnail:
                formats_list.append({
                    "format_id": "thumbnail",
                    "label": "Обложка",
                    "ext": "jpg",
                    "size": "Картинка",
                    "height": -1,
                    "is_audio": False
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

