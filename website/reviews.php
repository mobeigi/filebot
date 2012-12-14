<html>
	<head>
		<title>Reviews</title>
		<link rel="stylesheet" href="base.css" type="text/css" />
		<link href='http://fonts.googleapis.com/css?family=Dawning+of+a+New+Day' rel='stylesheet' type='text/css'>
		
		<!-- google analytics -->
		<script type="text/javascript">
			var _gaq = _gaq || [];
			_gaq.push(['_setAccount', 'UA-25379256-1']);
			_gaq.push(['_trackPageview']);
			
			(function() {
				var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
				ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
				var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
			})();
		</script>
	</head>
	<body>
		<div class="top"><small>Write a review for FileBot at <a href="https://sourceforge.net/projects/filebot/reviews/?sort=usefulness#review-form">SourceForge.net</a></small></div>
		<?
		// read file
		$filename = "reviews.json";
		$handle = fopen($filename, "r");
		$contents = fread($handle, filesize($filename));
		fclose($handle);

		// parse Json
		$reviews = json_decode($contents);
		shuffle($reviews);

		foreach ($reviews as $review) {
			?>
			<div class="review message box" style="float:left">
				<div class="thumbs up">
					<img src="images/thumbs_up.png" />
				</div>
				<div>
					<p class="head">Posted by <span class="user"><? echo $review->user ?></span> on <span class="date"><? echo $review->date ?></span></p>
					<p class="text quote"><? echo $review->text ?></p>
				</div>
			</div>
			<?
		}
		?>
	</body>
</html>
