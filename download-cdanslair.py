#!/usr/bin/python -tt
import sys
import gettext
import locale
import logging
import urllib2
import datetime
import subprocess
import shutil
import os
import argparse
# My own modules
sys.path.append(os.path.join(os.path.dirname(os.path.realpath(sys.argv[0])), 'pym'))
import urllister
import metataglister
from xml.dom import minidom

# RSS FEED
RSS_URL = "http://feeds.feedburner.com/france5/Gxsc?format=xml"

def main():
  # Parse command line
  parser = argparse.ArgumentParser(description=_('Downloads the newly available cdanslair episodes.'))
  parser.add_argument('-d', '--dir', action='store', metavar='directory', default='.', help=_('The directory where to store the media files'))
  parser.add_argument('-f', '--force', action="store_true", help=_('Force new download for incomplete files'))
  parser.add_argument('-c', '--check', action="store_true", help=_('Check for new episodes only, but do not download'))
  args = parser.parse_args()

  global work_folder
  # Checking work folder given in argument
  if(args.dir):
    if(os.path.exists(args.dir)):
      work_folder = os.path.abspath(args.dir)
    else:
      print _("Directory %s does not exist. Exiting.") % args.dir
      exit()

  # Downloading latest RSS file
  episodes = parseRssFeed(RSS_URL)
  if(episodes == -1):
    print _("Could not fetch the RSS feed. Exiting.")
    exit()

  # Check for new episodes
  tmp_s = _("Checking for (%d) new episodes: ") % len(episodes)
  os.write(1,tmp_s)
  epToDownload = checkNewEpisodes(episodes, args.force, True)

  # Download media files
  if(not args.check):
    for media in epToDownload:
      # Check if file is already here or if it is incomplete (with --force-inc option)
      if not isFileAlreadyHere(media.filename) or (args.force and isFileComplete(media.filename)):
        downloadStream(media)

  if(len(epToDownload) == 0):
    print _("All medias have already been downloaded, nothing to do.")
  elif(args.check):
    print _("There would be %d episodes to download:") % len(epToDownload)
    for ep in epToDownload:
      print "* " + ep.title
  else:
    print _("Done.")

def checkNewEpisodes(episodes, force, should_print):
  toDownload = []
  for ep in episodes:
    # For some reasons, fetching the URL takes time (slow server response),
    # so we should check first whether we already have the file.
    # If --force-inc option is selected and file not completed, we fetch the link too
    if not isFileAlreadyHere(ep.filename) or (force and isFileComplete(ep.filename)):
      ep.fetchMediaLink()
      if(ep.mediaLink == -1 and should_print):
        os.write(1,"?")
      else:
        if(should_print):
          os.write(1,"@")
        toDownload.append(ep)
    elif(should_print):
      os.write(1,"#")
  if(should_print):
    os.write(1,"\n");
  return toDownload

def downloadStream(media):
  print media.title #title
  media.filename = media.filename + u"_" +  media.title.replace(" ", "-")
  # TODO handle mplayer "connection refused" --> we need to delete the file
  fullPath = os.path.join(work_folder, (u"%s" % media.filename))
  try:      
    process = subprocess.Popen(["mplayer", "-dumpstream", media.mediaLink, "-dumpfile", fullPath], shell=False) #os.system(cmd) --> issue with unicode chars
    process.wait()
  except KeyboardInterrupt: #Ctrl-C
    process.terminate()
    print _("The process has been interrupted --> renaming the file to %s") % "NOT FINISHED"
    shutil.move(fullPath, work_folder + "/" + "NOT_FINISHED_" + media.filename)
  except:
    print _("Oops, something unexpected happened while running mplayer")
  else:
    print _(" %s, %s%sFile already present. No need for downloading.") % (media.date, media.title, '\n\t')

def isFileAlreadyHere(filename):
  for aFile in os.listdir(work_folder):
    # Trunking the file name to the original file name ie cdanslair_YYYYMMDD
    if aFile[:18] == filename:
      return True
  # If no match found the file is no present in the folder
  return False

def isFileComplete(filename):
  '''Checks whether a file looks complete, i.e. more than 500 MB'''
  SIZE_COMPLETE = 500000 # 500 MB
  for aFile in os.listdir(work_folder):
    # Trunking the file name to the original file name ie cdanslair_YYYYMMDD
    if aFile[:18] == filename:
      statinfo = os.stat(os.path.join(work_folder, aFile))
      if(statinfo.st_size > SIZE_COMPLETE):
        return True
  # If no match found the file is no present in the folder
  return False

def clickExtractAndBuildUrl(ep_url):
  MMS_BEGINNING = "mms://a988.v101995.c10199.e.vm.akamaistream.net/7/988/10199/3f97c7e6/ftvigrp.download.akamai.com/10199/cappuccino/production/publication/"
  try:
    ep_sock = urllib2.urlopen(ep_url) 
  except IOError:
    return -1;
  parser = urllister.URLLister()
  parser.feed(ep_sock.read())
  ep_sock.close()
  parser.close()
  for url in parser.urls:
    # I am looking for a URL like this:
    # http://info.francetelevisions.fr/video-info/?id-video=rhozet_cdanslair_20111104_455_04112011182935_F5
    if url.startswith("http://info.francetelevisions.fr/video-info/?id-video="):
      media_stream = getMediaPath(url[url.index('=')+1:])
      if(media_stream == -1 or media_stream == None):
        return -1
      return MMS_BEGINNING + media_stream
  print _("Could not find the id-video link in the page. This probably means that the video has not been published yet.")
  return -1

def getMediaPath(external_id):
  '''Makes an extra query to find out the media path on the server'''
  # id-externe=rhozet_c_dans_lair_20130212_455_12022013191331_F5
  info_url = "http://www.france5.fr/appftv/webservices/video/getInfosVideo.php?src=cappuccino&video-type=simple\
&template=ftvi&template-format=complet&id-externe=" + external_id
  try:
    url_sock = urllib2.urlopen(info_url)
  except IOError:
    return -1;
  xmldoc = minidom.parse(url_sock)  
  url_sock.close()
  # It should be safe to assume there is only one 'fichier' node
  element = xmldoc.getElementsByTagName("element")[0]
  video = element.getElementsByTagName("video")[0]
  if(video == None):
    print _("Media file not found on the server")
    return -1
  fichier = video.getElementsByTagName("fichiers")[0].getElementsByTagName("fichier")[0]
  name = fichier.getElementsByTagName("nom")[0].firstChild.data
  path = fichier.getElementsByTagName("chemin")[0].firstChild.data
  return path + name;

def parseRssFeed(rss_url):
  # Refactored w/ http://www.blog.pythonlibrary.org/2010/11/12/python-parsing-xml-with-minidom/
  try:
    url_sock = urllib2.urlopen(rss_url)
  except IOError:
    return -1;
  xmldoc = minidom.parse(url_sock)  
  url_sock.close()
  items = xmldoc.getElementsByTagName("item")
  episodes = [] #storing the episodes properties
  for item in items:
    #We found an episode! Retrieving data from the tags.
    pubDateObj = item.getElementsByTagName("pubDate")[0]
    date = pubDateObj.firstChild.data
    linkObj = item.getElementsByTagName("link")[0]
    url = linkObj.firstChild.data
    titleObj = item.getElementsByTagName("title")[0]
    title = titleObj.firstChild.data
    descObj = item.getElementsByTagName("description")[0]
    desc = descObj.firstChild.data
    #Building the Episode object
    ep = Episode(url,date)
    ep.title = title
    ep.desc = desc
    episodes.append(ep)
  #Done with the parsing!
  return episodes

class Episode():
  '''represents an Episode'''
  #date, link, title, desc
  def __init__(self, url, date):
    self.url = url
    self.date = date
    self.setFilename()

  def fetchMediaLink(self):
    self.mediaLink = clickExtractAndBuildUrl(self.url)
    self.setFilename()

  def setFilename(self):
    # print "User locale is:" + locale.getlocale(locale.LC_TIME)[0]
    # Change time locale to US for rss date parsing
    locale.setlocale(locale.LC_TIME,("en_US","utf8"))
    try:
      extractedDate = datetime.datetime.strptime(self.date, "%a, %d %b %Y %H:%M:%S +0000") # <pubDate>Thu, 03 Nov 2011 23:00:00 +0000</pubDate>
      extractedDate = extractedDate + datetime.timedelta(days=1) # The pubDate of this RSS feed is one day behind (i.e., published the day before)
      extractedDate = extractedDate.strftime("%Y%m%d")
      self.filename = u"cdanslair_%s" % extractedDate
    except ValueError:
      #extractedDate = datetime.datetime.strptime(self.date[5:-15], "%d %b %Y")
      print "Could not parse date properly %s" % self.date
      self.filename = u"cdanslair_%s" % self.date[5:-15]
    # Reverts locale back to user locale
    locale.resetlocale(locale.LC_TIME)

def init_localization():
  '''prepare l10n'''
  locale.setlocale(locale.LC_ALL, '') # use user's preferred locale
  # take first two characters of country code
  loc = locale.getlocale()
  filename = os.path.join(base_dir, "res/messages_%s.mo" % loc[0][0:2])
 
  try:
    logging.debug( "Opening message file %s for locale %s", filename, loc[0] )
    trans = gettext.GNUTranslations(open( filename, "rb" ) )
  except IOError:
    logging.debug( "Locale not found. Using default messages" )
    print  "Locale not found. Using default messages"
    trans = gettext.NullTranslations()
 
  trans.install()

if __name__ == '__main__':
  global base_dir
  base_dir = os.path.normpath(os.path.dirname(os.path.realpath(sys.argv[0])))
  init_localization()
  main()
