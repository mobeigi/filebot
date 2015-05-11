#!/bin/sh

for DUMP in "omdb.zip" "tvdb.zip" "anidb.gz" "osdb.gz"
do
	if [ ! -f "$DUMP" ]; then
		touch -a -m -t 197001010000.00 "$DUMP"
	fi
done

echo 'Fetch OMDB dump'
if test "`find omdb.zip -mtime +20`"; then
    curl -L -o omdb.zip -z omdb.zip "http://beforethecode.com/projects/omdb/download.aspx?e=reinhard.pointner%40gmail.com&tsv=movies"
    unzip -o omdb.zip
fi

echo 'Fetch TVDB dump'
if test "`find tvdb.zip -mtime +5`"; then
    curl -L -o tvdb.zip -z tvdb.zip "http://thetvdb.com/api/58B4AA94C59AD656/updates/updates_all.zip"
    unzip -o tvdb.zip
fi

echo 'Fetch AniDB dump'
if test "`find anidb.gz -mtime +5`"; then
    curl -L -o anidb.gz -z anidb.gz "http://anidb.net/api/anime-titles.dat.gz"
fi

echo 'Fetch OSDB dump'
if test "`find osdb.gz -mtime +20`"; then
    curl -L -o osdb.gz -z osdb.gz "http://www.opensubtitles.org/addons/export_movie.php" -sH 'Accept-encoding: gzip'
    gunzip -c osdb.gz > osdb.txt
fi
