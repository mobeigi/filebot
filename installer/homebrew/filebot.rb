require "formula"

class Filebot < Formula
  homepage "http://www.filebot.net/"
  url "http://sourceforge.net/projects/filebot/files/filebot/FileBot_4.1/FileBot_4.1.app.tar.gz"
  sha1 "1a0363b9a7bfa2bbd7d6c81d22a0c8e154c8ce81"
  version "4.1"

  def install
    # Create .app bundle in prefix
    (prefix/'FileBot.app').install Dir['*']

    # Create filebot symlink in bin
    bin.install_symlink prefix/'FileBot.app/Contents/MacOS/filebot.sh' => 'filebot'
  end
  
  def post_install
    # Clearing cache and temporary files
    system "#{bin}/filebot", "-clear-cache"
    # Initializing Cache
    system "#{bin}/filebot", "-script", "g:MediaDetection.warmupCachedResources()"
  end

  test do
    system "#{bin}/filebot", "-version"
  end
end
