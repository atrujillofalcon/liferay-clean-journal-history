# liferay-clean-journal-history
Groovy script that makes possible to delete or only list old journal articles and its resources from Liferay database. You can set the amount of versions by journal article to save modifying the variable <b>leaveVersionCount</b>. 

The master branch is only compatible with Liferay DXP (7 onfoward), if you want to use this script on older versions you have to get the 6.2 branch. 

If you only want to list the journal articles targeted to be deleted you have to set false the variable <b>deleteArticles</b>.

To process a specific sites, you can set explicitly the groupId's in the variable <b>groupIds</b>, separated by comma.

The steps to execute this script are:

<b>Control_Panel -> Server_Administration -> Script ->  Groovy Script</b>
