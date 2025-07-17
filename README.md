# What is this project?
iStream is a Server-Client Java project that using a secure connection TLS/SSL with encryption keys. The application allows the client to select a video from the Server's Catalog and stream it directly without downloading the video. The client features adaptive streaming, video clipping and more.

### It implements:

  1. ffmpeg wrapping (Server)
	
  2. javaFX (Client)
	
  3. Multithreading (Server)
	
  4. Logging (Server|Client)
	
  5. Process builder for commands (Server|Client)
	
  6. Video protocol transmission, TCP, UDP, RTP/UDP (Server|Client)
	
  7. Adaptive streaming (Server|Client)
	
  8. Load Balancing (Server)
	
  9. Fplay Recording (Client)
  

	
### Requirements:

  1. Any video (Server side) in "AvailableVideos/" must be named as followed: "x"-"y"p."z" where x is the video name, y is the display and z is the extention. (For example "Surfing-720p.mp4")
	
  2. Both Client and Server should have ffmpeg extracted in the following directory: "C:/ffmpeg/bin/" (To update|Fix)
	
  3. In order for the client sided fplay recording to include sound "Stream box" from realtek's drivers are recommended.
	

#### This project was conducted without collaborators, and is a clear demonstration of java skills.
