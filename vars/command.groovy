import java.util.UUID

//A function that executes a shell or bat command and returns its output. 
def call(command) {
	def uuid = UUID.randomUUID()
	def filename = "cmd-${uuid}"
	def cmd = "${command} > ${filename}"
	isUnix()?sh(cmd):bat(cmd)
	def result = readFile(filename).trim()
	isUnix()?sh("rm ${filename}"):bat("del ${filename}")
	return result
}
