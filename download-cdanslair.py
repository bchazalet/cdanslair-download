#!/usr/bin/python -tt
import sys
import urllib2
import datetime
import subprocess
import shutil
import os
# My own modules
sys.path.append(os.path.join(os.path.dirname(os.path.realpath(sys.argv[0])), 'pym'))
import urllister
import metataglister
from xml.dom import minidom

# Where the downloaded medias are stored
WORK_FOLDER = "/home/bchazalet/cdanslair" 

def main():
	#download new RSS file from website
	print "Fetching RSS"
	#rss_url = "http://feeds.feedburner.com/france5/cdanslair?format=xml"
	#rss_url = "http://www.pluzz.fr/c-dans-l-air/rss"
	rss_url = "http://feeds.feedburner.com/france5/Gxsc?format=xml"
	episodes = parseRssFeed(rss_url)
	if(episodes == -1):
		exit()

	toDownload = []
	os.write(1,"Checking and fetching (" + str(len(episodes))+ "): ")
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
			fullPath = u"%s" % (WORK_FOLDER + "/" + media.filename)
			try:			
				process = subprocess.Popen(["mplayer", "-dumpstream", media.mediaLink, "-dumpfile", fullPath], shell=False) #os.system(cmd) --> issue with unicode chars
				process.wait()
			except KeyboardInterrupt: #Ctrl-C
				process.terminate()
				print "The process has been interrupted --> renaming the file to NOT FINISHED"
				shutil.move(fullPath, WORK_FOLDER + "/" + "NOT_FINISHED_" + media.filename)
			except:
				print "Oops, something unexpected happened while running mplayer"
			#print "Mplayer return code: " + str(process.returncode)
		else:
			print media.date + ", " + media.title + "\n\tFile already present. No need for downloading."

	if(len(toDownload) == 0):
		print "All medias have already been downloaded, nothing to do."
	else:
		print "Done."

def isFileAlreadyHere(filename):
	for aFile in os.listdir(WORK_FOLDER):
		#TRunking the file name to the original file name ie cdanslair_YYYYMMDD
		if aFile[:18] == filename:
			return True
	#If no match found the file is no present in the folder
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
		# I am looking for a URL like this
		# http://info.francetelevisions.fr/?id-video=rhozet_cdanslair_20111024_455_24102011180940_F5
		# New one: http://info.francetelevisions.fr/video-info/?id-video=rhozet_cdanslair_20111104_455_04112011182935_F5
		if url.startswith("http://info.francetelevisions.fr/video-info/?id-video="):
			# Now we need to look for <meta name="urls-url-video" content="Autre/Autre/2011/S43/J1/332858_cdanslair_20111024.wmv" >
			wmvResource = getContentOfMeta(url, "urls-url-video")
			if(wmvResource == -1 or wmvResource == None):
				return -1
			return MMS_BEGINNING + wmvResource
	print "Could not find the id-video link in the page"
	return -1

def getContentOfMeta(url, metaId):
	#Now we need to look for <meta name="urls-url-video" content="Autre/Autre/2011/S43/J1/332858_cdanslair_20111024.wmv" >
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
		print "Could not fetch the RSS feed. Closing."
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
	#date, link, title, desc
	def __init__(self, url, date):
		self.url = url
		self.date = date
		self.setFilename()

	def fetchMediaLink(self):
		self.mediaLink = clickExtractAndBuildUrl(self.url)
		self.setFilename()

	def setFilename(self):
		#last_slash = self.mediaLink.rfind("/",0,len(self.mediaLink));
		#self.filename = self.mediaLink[last_slash+1+7:-4] #without the extension
		#extractedDate = self.url[33:44].replace("-","");
		extractedDate = datetime.datetime.strptime(self.date, "%a, %d %b %Y %H:%M:%S +0000") # <pubDate>Thu, 03 Nov 2011 23:00:00 +0000</pubDate>
		extractedDate = extractedDate + datetime.timedelta(days=1) # The pubDate of this RSS feed is one day behind (i.e., published the day before)
		extractedDate = extractedDate.strftime("%Y%m%d")
		self.filename = u"cdanslair_%s" % extractedDate

if __name__ == '__main__':
	main()

##### OLD STUFFS THAT I DON'T WANT TO LOSE #####

# Not in use anymore, now using the meta tag
#def getMmsLinkFromUrl(ep_url):
#	try:
#		ep_sock = urllib2.urlopen(ep_url)	
#	except IOError:
#		#print "Could not fetch the page. Skipping to next one."
#		return -1;
#	parser = urllister.URLLister()
#	parser.feed(ep_sock.read())
#	ep_sock.close()
#	parser.close()
#	for url in parser.urls:
#		if url[:3] == "mms":
#			#medias.append((ep[0],url,file_name,ep[2],ep[3]))
#			return url # break # there is only one mms link per page
