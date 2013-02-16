This script automatically downloads the new episodes of the french TV program [cdanslair](http://www.france5.fr/c-dans-l-air).

It relies on its RSS feed to get the new episodes, fetch the episode page and look for the link pointing to the episode media stream.

It then uses mplayer with -dumpstream option to download the stream and save it in the same folder.

## Installation & launch for Ubuntu 12.10
You will need to install mplayer:   
`sudo apt-get install mplayer`

You'll need python:    
`sudo apt-get install python`

That's it, now you can run the script:    
`python download-cdanslair.py`

