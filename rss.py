#!/usr/bin/python -tt

import sys
import urllib
import os
import urllister
from xml.dom import minidom

def main():
	#download new RSS file from websit e
	print "Fetching RSS"
	rss_url = "http://feeds.feedburner.com/france5/cdanslair?format=xml"
	try:
		url_sock = urllib.urlopen(rss_url)
	except IOError:
		print "Could not fetch the RSS feed. Closing."
		return -1;
	#xml_file = usock.read()
	#print xml_file
	xmldoc = minidom.parse(url_sock)	
	url_sock.close()
	episodes = [] #storing the episodes properties
	for topNode in xmldoc.childNodes:
		if topNode.nodeName == "rss":
			for subTopNode in topNode.childNodes:
				if subTopNode.nodeName == "channel":
					for item in subTopNode.childNodes:
						if item.nodeName == "item":
							#We found an episode!
							#print "We found an episode!"
							date = ""
							link = ""
							title = ""
							desc = ""
							for prop in item.childNodes:
								if prop.nodeName == "pubDate":
									date = prop.firstChild.data
								elif prop.nodeName == "link":
									link = prop.firstChild.data
								elif prop.nodeName == "title":
									title = prop.firstChild.data
								elif prop.nodeName == "description":
									desc = prop.firstChild.data
							ep_prop = (date,link,title,desc)
							episodes.append(ep_prop)
	#Done with the parsing!
	
	#GET the episode webpage and find the MMS link
	os.write(1,"Fetching the mms links (" + str(len(episodes)) + ")")
	medias = []
	for ep in episodes:
		#print "GETing EP's page: " + ep[0] + " " + ep[1]
		try:
			ep_sock = urllib.urlopen(ep[1])	
		except IOError:
			#print "Could not fetch the page. Skipping to next one."
			os.write(1,"#")
			continue
		parser = urllister.URLLister()
		parser.feed(ep_sock.read())
		ep_sock.close()
		parser.close()
		for url in parser.urls:
			if url[:3] == "mms":
				last_slash = url.rfind("/",0,len(url));
				file_name = url[last_slash+1:-4] #without the extension
				medias.append((ep[0],url,file_name,ep[2],ep[3]))
				os.write(1,".")
				break # there is only one mms link per page
	os.write(1,"\n");

	#Start mplayer
	#How the url looks like
	#mms://a533.v55778.c5577.e.vm.akamaistream.net/7/533/5577/42c40fe4/lacinq.download.akamai.com/5577/internet/cdanslair/cdanslair_20110801.wmv
	for media in medias:
		#check if file is already here (in the folder)
		#print "Filename: " + media[2]
		#file_name.encode("utf-8")
		file_name = media[2]
		if not isFileAlreadyHere(file_name):
			print media[3] #title
			print media[4] #desc
			file_name = file_name # + "_" + shortDescForFileName(media[3])
			print file_name
			#TODO handle mplayer "connection refused" --> we need to delete the file
			cmd = "mplayer -dumpstream %s -dumpfile %s" % (media[1],file_name)
			os.system(cmd);
		else:
			print media[0] + ", " + media[3] + "\n\tFile already present. No need for downloading."

def isFileAlreadyHere(filename):
	for aFile in os.listdir("."):
		#TRunking the file name to the original file name ie cdanslair_YYYYMMDD
		if aFile[:18] == filename:
			return True
	#If no match found the file is no present in the folder
	return False

def shortDescForFileName(titre):
	tmp = titre.replace(" ","");
	tmp = tmp.replace("!","")
	#tmp = tmp.replace("","")
	tmp = tmp.replace("?","")
	tmp = tmp.replace("'","")
	tmp = tmp.replace(":","")
	tmp = tmp.replace("&","")
	tmp = tmp.replace("@","")
	if len(tmp) < 10:
		return tmp
	else:
		return tmp[:10]

if __name__ == '__main__':
	main()
