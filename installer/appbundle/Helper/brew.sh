#!/bin/sh -xu

# brew requires Command Line Tools for Xcode
xcode-select --print-path || sudo xcode-select --install

# install brew if necessary
brew info || ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"

# update brew package index and update all packages
brew update && brew upgrade

# install latest JDK
brew cask install java --force

# install FileBot bundle to ~/Applications
brew cask install filebot --force --appdir=~/Applications
