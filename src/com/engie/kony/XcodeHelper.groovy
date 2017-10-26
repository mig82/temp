package com.engie.kony

/* Traverse the xml to find the `dict` element corresponding to XPath:

		.//dict[key='TargetAttributes']/dict/key[text()='1D6058900D05DD3D006BFB54']/following-sibling::dict[1]
		
		OR

		./plist/dict/dict[2]/dict/dict[key='TargetAttributes']/dict/key[text()='1D6058900D05DD3D006BFB54']/following-sibling::dict[1]

	And add append to it:

		<key>ProvisioningStyle</key>
		<string>Manual</string>
*/
@NonCPS
def setManualProvisioningStyle(xml){

	def parser = new XmlParser(true, false, true)
	def plist = parser.parseText(xml) //The root element of the xml genereated from pbxproj is <plist>.
	assert plist.name() == 'plist'

	def plistDicts = plist['dict']
	assert plistDicts.size() == 1 //There should only be one child dict element to the plist.

	//Let's get the children elements and their indices. We need the indices to find the <dict> element next to each <key> element.	
	def children = plistDicts[0].children().withIndex()

	/* The children of plist/dict should be something like the following: children.collect{ it.name() }.withIndex()
		[
			[key, 0],		<key>archiveVersion</key>
			[string, 1],	<string>1</string>
			[key, 2],		<key>classes</key>
			[dict, 3],		<dict/>
			[key, 4],		<key>objectVersion</key>
			[string, 5],	<string>45</string>
			[key, 6],		<key>objects</key>
			[dict, 7],		<dict>							<-- This is the 'dict' we're looking for, just after the 'objects' key.
			[key, 8],		<key>rootObject</key>
			[string, 9]		<string>29B97313FDCFA39411CA2CEA</string>
		]
	*/

	//1. Let's find the <dict> element next to <key>objectVersion</key>. The ProvisioningStyle settings will be here.
	def objectsKeyIndex = children.find{ child ->
		child[0].name() == 'key' && child[0].text() == 'objects'
	}[1] //0 is the element, 1 is the index.
	def objectsDict = children[objectsKeyIndex + 1][0] //0 is the element, 1 is the index.
	assert objectsDict.name() == 'dict'

	//2. Let's find the <string> element next to <key>rootObject</key>. Its value is required to search through the doc tree. 
	def rootObjectKeyIndex = children.find{ child ->
		child[0].name() == 'key' && child[0].text() == 'rootObject'
	}[1]
	def rootString = children[rootObjectKeyIndex + 1][0] //0 is the element, 1 is the index.
	assert rootString.name() == 'string'
	String rootId = rootString.text() 
	echo("rootId: '${rootId}'.") //Should be 29B97313FDCFA39411CA2CEA
	
	//3. Let's find the <dict> element next to <key>29B97313FDCFA39411CA2CEA</key>
	def objectsChildren = objectsDict.children().withIndex()
	def rootIdKeyIndex = objectsChildren.find{ child -> 
		child[0].name() == 'key' && child[0].text() == rootId
	}[1] //0 is the element, 1 is the index.
	def rootIdDict = objectsChildren[rootIdKeyIndex + 1][0] //0 is the element, 1 is the index.
	assert rootIdDict.name() == 'dict'

	//4. Let's find the <dict> element next to <key>attributes</key>
	def rootIdChildren = rootIdDict.children().withIndex()
	def attributesKeyIndex = rootIdChildren.find{ child ->
		child[0].name() == 'key' && child[0].text() == 'attributes'
	}[1] //0 is the element, 1 is the index.
	def attributesDict = rootIdChildren[attributesKeyIndex + 1][0] //0 is the element, 1 is the index.
	assert attributesDict.name() == 'dict'

	//5. Let's find the <dict> element next to <key>TargetAttributes</key>
	def attributesChildren = attributesDict.children().withIndex()
	def targetAttributesKeyIndex = attributesChildren.find{ child ->
		child[0].name() == 'key' && child[0].text() == 'TargetAttributes'
	}[1] //0 is the element, 1 is the index.
	def targetAttributesDict = attributesChildren[targetAttributesKeyIndex + 1][0] //0 is the element, 1 is the index.
	assert targetAttributesDict.name() == 'dict'

	//6. TODO: Find the ID for the scheme we want to build -e.g. to determine that KRelease is 1D6058900D05DD3D006BFB54
	String schemeId = '1D6058900D05DD3D006BFB54'

	//7. Let's find the <dict> element next to the key with the scheme id we want to build -e.g. <key>1D6058900D05DD3D006BFB54</key> 
	def targetAttributesChildren = targetAttributesDict.children().withIndex()
	def schemeKeyIndex = targetAttributesChildren.find{ child ->
		child[0].name() == 'key' && child[0].text() == schemeId
	}[1] //0 is the element, 1 is the index.
	def schemeDict = targetAttributesChildren[schemeKeyIndex + 1][0] //0 is the element, 1 is the index.
	assert schemeDict.name() == 'dict'

	/* 8. Let's append:

			<key>ProvisioningStyle</key>
			<string>Manual</string>
	*/
	schemeDict.appendNode(
		new groovy.xml.QName("key"),
		[:],
		"ProvisioningStyle"
	)
	schemeDict.appendNode(
		new groovy.xml.QName("string"),
		[:],
		"Manual"
	)
	return groovy.xml.XmlUtil.serialize(plist)
}