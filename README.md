CacheProxyServer
================

Local server storage media played by mediaplayer android.


What does the server, using existing libraries, is to mediate the transfer 
of media files requested by the player.

As the proxy server is who makes the transfer of media files, the target 
url should be passed to it.

The server takes care of redirecting the flow of data to the mediaplayer and 
also stores the file in the cache directory of private the application.

```
  ...
  
  cacheProxyServer = new CacheProxyServer({Context});
  cacheProxyServer.listen();
  
  String targetUrl = "http://music18.som13.com/2766f9f75771e1ddbbbc3fcc29f36801";
  String url = cacheProxyServer.getAbsoluteUrl("remoteFile", targetUrl); // base64 encoded
  
  MediaPlayer player = new MediaPlayer();
  player.reset();
  
  player.setDataSource(url);
  ...
```

Extent to which the file is being downloaded will be stored and saved for a new request.

http://developer.android.com/reference/android/media/MediaPlayer.html
