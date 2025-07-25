# What is iStream?
iStream is a Server-Client Java project that uses a secure connection TLS/SSL with encryption keys. The application allows the client to select a video from the Server's Catalog and stream it directly without downloading the video. The client features adaptive streaming, video clipping and more.

### It implements:

  - ffmpeg wrapping (Server)
	
  - javaFX (Client)
	
  - Multithreading (Server)
	
  - Logging (Server | Client)
	
  - Process builder for commands (Server | Client)
	
  - Video protocol transmission, TCP, UDP, RTP/UDP (Server | Client)
	
  - Adaptive streaming (Server | Client)
	
  - Load Balancing (Server)
	
  - Fplay Recording (Client)
  

	
## Requirements:

  1. Any video (Server side) in "AvailableVideos/" must be named as followed: "x"-"y"p."z" where x is the video name, y is the display and z is the extention. For example:
	<pre> Surfing-720p.mp4 </pre>
	
  2. - If you are using **Windows**, make sure you have ffmpeg, ffprobe and ffplay as environment variables. For example:
       
     <pre> ffmpeg  => C:/ffmpeg/bin/ffmpeg.exe </pre>
     <pre> ffprobe => C:/ffmpeg/bin/ffprobe.exe </pre>
     <pre> ffplay  => C:/ffmpeg/bin/ffplay.exe </pre>
     Replace "C:/ffmpeg/bin/" with your actual path
     
     - If you are on **Linux** make sure ffmpeg is installed using the following command:
       
     <pre>sudo apt install ffmpeg</pre>
	
  3. In order for the client sided Video recording to include sound, "Stream box" from realtek's drivers are recommended.
  


*Side note: You can run the server just by running the loadbalancer*

**This project was conducted without collaborators, and is a clear demonstration of java skills.**
