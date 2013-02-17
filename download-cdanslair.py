#!/usr/bin/python -tt

__author__ = "Boris Chazalet"

import sys
import gettext
import locale
import logging
import subprocess
import shutil
import os
import argparse
# My own modules
sys.path.append(os.path.join(os.path.dirname(os.path.realpath(sys.argv[0])), 'pym'))
import cdanslair

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
  episodes = cdanslair.parseRssFeed()
  if(episodes == -1):
    print _("Could not fetch the RSS feed. Exiting.")
    exit()

  # Check for new episodes
  tmp_s = _("Checking for (%d) new episodes: ") % len(episodes)
  os.write(1,tmp_s)
  epToDownload = cdanslair.checkNewEpisodes(work_folder, episodes, args.force, True)

  # Download media files
  if(not args.check):
    for media in epToDownload:
      # Check if file is already here or if it is incomplete (with --force-inc option)
      if not cdanslair.isFileAlreadyHere(work_folder, media.filename) or (args.force and cdanslair.isFileComplete(work_folder, media.filename)):
        downloadStream(media)

  if(len(epToDownload) == 0):
    print _("All medias have already been downloaded, nothing to do.")
  elif(args.check):
    print _("There would be %d episode(s) to download:") % len(epToDownload)
    for ep in epToDownload:
      print "* " + ep.title
  else:
    print _("Done.")

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
