#!/bin/sh

echo 'Fetch TVDB dump'
DUMP='tvdb.zip'
LINK='http://thetvdb.com/api/58B4AA94C59AD656/updates/updates_all.zip'
if [ ! -f "$DUMP" ] || test "`find $DUMP -mtime +5`"; then
    curl -L -o "$DUMP" -z "$DUMP" "$LINK"
    unzip -o "$DUMP"
fi

echo 'Fetch AniDB dump'
DUMP='anidb.gz'
TEXT='anidb.txt'
LINK='http://anidb.net/api/anime-titles.dat.gz'
if [ ! -f "$DUMP" ] || test "`find $DUMP -mtime +5`"; then
    curl -L -o "$DUMP" -z "$DUMP" "$LINK"
    gunzip -c "$DUMP" > "$TEXT"
fi

echo 'Fetch OSDB dump'
DUMP='osdb.gz'
TEXT='osdb.txt'
LINK='http://www.opensubtitles.org/addons/export_movie.php'
if [ ! -f "$DUMP" ] || test "`find $DUMP -mtime +30`"; then
    curl -L -o "$DUMP" -z "$DUMP" "$LINK" -sH 'Accept-encoding: gzip'
    gunzip -c "$DUMP" > "$TEXT"
fi

echo 'Fetch OMDB dump'
DUMP='omdb.zip'
LINK='http://beforethecode.com/projects/omdb/download.aspx?e=reinhard.pointner%40gmail.com&tsv=movies'
if [ ! -f "$DUMP" ] || test "`find $DUMP -mtime +30`"; then
    curl -L -o "$DUMP" -z "$DUMP" "$LINK"
    unzip -o "$DUMP"
fi

echo 'DONE'
