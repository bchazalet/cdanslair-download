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

# Where the downloaded medias are stored
RSS_URL = "http://feeds.feedburner.com/france5/Gxsc?format=xml"

def main():
  # Parse command line
  parser = argparse.ArgumentParser(description=_('Downloads the newly available cdanslair episodes.'))
  parser.add_argument('-d', '--dir', action='store', metavar='directory', default='.', help=_('The directory where to store the media files'))
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
  #print "Fetching RSS"
  episodes = parseRssFeed(RSS_URL)
  if(episodes == -1):
    print _("Could not fetch the RSS feed. Exiting.")
    exit()

  toDownload = []
  os.write(1,_("Checking and fetching (%s): ") % str(len(episodes)))
  for ep in episodes:
    #For some reasons, fetching the URL takes time (slow server response), so we should check already if we already have the file.
    if not isFileAlreadyHere(ep.filename):
      ep.fetchMediaLink()
      if(ep.mediaLink == -1):
        os.write(1,"?")
      else:
        os.write(1,".")
        toDownload.append(ep)
    else:
      os.write(1,"#")
      #print "\n" + ep.date + ", " + ep.title + "\n\tFile already present. No need for fetching MMS link and downloading."
  os.write(1,"\n");

  #Start mplayer
  #How the url looks like
  #mms://a533.v55778.c5577.e.vm.akamaistream.net/7/533/5577/42c40fe4/lacinq.download.akamai.com/5577/internet/cdanslair/cdanslair_20110801.wmv
  for media in toDownload:
    #check if file is already here (in the folder)
    if not isFileAlreadyHere(media.filename):
      print media.title #title
      media.filename = media.filename + u"_" +  media.title.replace(" ", "-")
      #print media.desc #desc # do not display unless we can parse it before (html)
      #TODO handle mplayer "connection refused" --> we need to delete the file
      fullPath = u"%s" % (work_folder + "/" + media.filename)
      try:      
        process = subprocess.Popen(["mplayer", "-dumpstream", media.mediaLink, "-dumpfile", fullPath], shell=False) #os.system(cmd) --> issue with unicode chars
        process.wait()
      except KeyboardInterrupt: #Ctrl-C
        process.terminate()
        print _("The process has been interrupted --> renaming the file to %s") % "NOT FINISHED"
        shutil.move(fullPath, work_folder + "/" + "NOT_FINISHED_" + media.filename)
      except:
        print _("Oops, something unexpected happened while running mplayer")
      #print "Mplayer return code: " + str(process.returncode)
    else:
      print _(" %s, %s%sFile already present. No need for downloading.") % (media.date, media.title, '\n\t')

  if(len(toDownload) == 0):
    print _("All medias have already been downloaded, nothing to do.")
  else:
    print _("Done.")

def isFileAlreadyHere(filename):
  for aFile in os.listdir(work_folder):
    # Trunking the file name to the original file name ie cdanslair_YYYYMMDD
    if aFile[:18] == filename:
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
    # New one: http://info.francetelevisions.fr/video-info/?id-video=rhozet_cdanslair_20111104_455_04112011182935_F5
    if url.startswith("http://info.francetelevisions.fr/video-info/?id-video="):
      # Now we need to look for <meta name="urls-url-video" content="Autre/Autre/2011/S43/J1/332858_cdanslair_20111024.wmv" >
      wmvResource = getContentOfMeta(url, "urls-url-video")
      if(wmvResource == -1 or wmvResource == None):
        return -1
      return MMS_BEGINNING + wmvResource
  print _("Could not find the id-video link in the page. This probably means that the video has not been published yet.")
  return -1

def getContentOfMeta(url, metaId):
  # Now we need to look for <meta name="urls-url-video" content="Autre/Autre/2011/S43/J1/332858_cdanslair_20111024.wmv" >
  try:
    ep_sock = urllib2.urlopen(url)  
  except IOError:
    return -1;
  parser = metataglister.MetaTagLister()
  parser.feed(ep_sock.read())
  ep_sock.close()
  parser.close()
  for metaTag in parser.metaTags:
    nameAttr = metaTag[0]
    attrName =  nameAttr[0]
    attrValue = nameAttr[1]
    if attrName == "name" and attrValue == "urls-url-video":
      contentAttr = metaTag[1]
      return contentAttr[1]

def parseRssFeed(rss_url):
  # Refactored w/ http://www.blog.pythonlibrary.org/2010/11/12/python-parsing-xml-with-minidom/
  try:
    url_sock = urllib2.urlopen(rss_url)
  except IOError:
    return -1;
  xmldoc = minidom.parse(url_sock)  
  url_sock.close()
  node = xmldoc.documentElement
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
    extractedDate = datetime.datetime.strptime(self.date, "%a, %d %b %Y %H:%M:%S +0000") # <pubDate>Thu, 03 Nov 2011 23:00:00 +0000</pubDate>
    extractedDate = extractedDate + datetime.timedelta(days=1) # The pubDate of this RSS feed is one day behind (i.e., published the day before)
    extractedDate = extractedDate.strftime("%Y%m%d")
    self.filename = u"cdanslair_%s" % extractedDate

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
