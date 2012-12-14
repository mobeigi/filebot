
/**
 * Log into a remote host and run a given command.
 * 
 * e.g. 
 * sshexec(command: "ps", host: "filebot.sf.net", username: "rednoah", password: "correcthorsebatterystaple")
 */
def sshexec(param) {
	param << [trust: true] // auto-trust remote hosts
	
	_guarded {
		ant().sshexec(param)
	}
}


/**
 * Send email via smtp.
 * 
 * e.g. 
 * sendmail(mailhost:'smtp.gmail.com', mailport:'587', ssl:'no', enableStartTLS:'yes', user:'rednoah@gmail.com', password:'correcthorsebatterystaple', from:'rednoah@gmail.com', to:'someone@gmail.com', subject:'Hello Ant World', message:'Dear Ant, ...')
 */
def sendmail(param) {
	def sender    = param.remove('from')
	def recipient = param.remove('to')
	
	_guarded {
		ant().mail(param) {
			from(address:sender)
			to(address:recipient)
		}
	}
}


/**
 * Send email using gmail default settings.
 *
 * e.g.
 * sendGmail(subject:'Hello Ant World', message:'Dear Ant, ...', to:'someone@gmail.com', user:'rednoah', password:'correcthorsebatterystaple')
 */
def sendGmail(param) {
	param << [mailhost:'smtp.gmail.com', mailport:'587', ssl:'no', enableStartTLS:'yes']
	param << [user:param.username ? param.remove('username') + '@gmail.com' : param.user]
	param << [from: param.from ?: param.user]
	
	sendmail(param)
}


def ant() {
	return new AntBuilder()
}
