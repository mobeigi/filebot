#!/bin/sh

LOGO='artwork_4096x4096.png'

for SIZE in 358x173 358x358 1000x800 414x180 414x468 558x558 558x756 846x468 2400x1200; do
	convert -verbose $LOGO -resize $SIZE -gravity center -background transparent -extent $SIZE "artwork_$SIZE.png" 
done
