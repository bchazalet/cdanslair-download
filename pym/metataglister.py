#!/usr/bin/python -tt

from sgmllib import SGMLParser

class MetaTagLister(SGMLParser):
  def reset(self):
    SGMLParser.reset(self)
    self.metaTags = []

  def start_meta(self, attrs): #["name","urls-url-video"] 
    #meta = [v for k, v in attrs if k=='name']
    #nameAttr = attrs[0]
    #if nameAttr == ("name", "urls-url-video"):
    #tag = OneMetaTag(attrs)
    self.metaTags.append(attrs)

#class OneMetaTag():
# def __init__(self, attrs):
#   self.attrs = attrs

if __name__ == "__main__":
  import urllib
  usock = urllib.urlopen("http://diveintopython.org/")
  parser = URLLister()
  parser.feed(usock.read())
  parser.close()
  usock.close()
  for metaTags in parser.metaTags: print metaTags[0] + "" + metaTags[1]
