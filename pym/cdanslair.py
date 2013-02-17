
__author__ = "Boris Chazalet"

import urllib2
import locale
import datetime
import os
from xml.dom import minidom
# My own modules
import urllister

# RSS FEED
RSS_URL = "http://feeds.feedburner.com/france5/Gxsc?format=xml"

class Episode():
  '''represents a cdanslair Episode'''
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


def parseRssFeed():
  # Refactored w/ http://www.blog.pythonlibrary.org/2010/11/12/python-parsing-xml-with-minidom/
  try:
    url_sock = urllib2.urlopen(RSS_URL)
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

def checkNewEpisodes(work_folder, episodes, force, should_print):
  toDownload = []
  for ep in episodes:
    # For some reasons, fetching the URL takes time (slow server response),
    # so we should check first whether we already have the file.
    # If --force-inc option is selected and file not completed, we fetch the link too
    if not isFileAlreadyHere(work_folder, ep.filename) or (force and isFileComplete(work_folder, ep.filename)):
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

def isFileAlreadyHere(work_folder, filename):
  for aFile in os.listdir(work_folder):
    # Trunking the file name to the original file name ie cdanslair_YYYYMMDD
    if aFile[:18] == filename:
      return True
  # If no match found the file is no present in the folder
  return False

def isFileComplete(work_folder, filename):
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