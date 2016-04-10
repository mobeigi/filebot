#!/bin/sh

fetch()
{
	FILE="$1"
	LINK="$2"
	TIME="$3"

	echo "Fetch $FILE"
	if [ ! -f "$FILE" ] || test "`find $FILE -mtime $TIME`"; then
		curl -L -o "$FILE" -z "$FILE" "$LINK"

		if [[ "${FILE##*.}" == "gz" ]]; then
			gunzip -k -f "$FILE"
		fi
		if [[ "${FILE##*.}" == "zip" ]]; then
			7z e -y "$FILE"
		fi
	fi
}

fetch anidb.txt.gz 'http://anidb.net/api/anime-titles.dat.gz' +5
fetch tvdb.zip 'http://thetvdb.com/api/58B4AA94C59AD656/updates/updates_all.zip' +5
fetch omdb.zip 'http://beforethecode.com/projects/omdb/download.aspx?e=reinhard.pointner%40gmail.com&tsv=movies' +30
fetch osdb.txt 'http://www.opensubtitles.org/addons/export_movie.php' +30

fetch anime-list.xml 'https://raw.githubusercontent.com/ScudLee/anime-lists/master/anime-list.xml' +30
fetch anime-movieset-list.xml 'https://raw.githubusercontent.com/ScudLee/anime-lists/master/anime-movieset-list.xml' +30

echo 'DONE'
