import com.liferay.portal.kernel.log.Log
import com.liferay.portal.kernel.log.LogFactoryUtil
import com.liferay.portal.kernel.dao.orm.*
import com.liferay.portal.kernel.util.ListUtil
import com.liferay.portal.kernel.util.PortalClassLoaderUtil
import com.liferay.portal.kernel.workflow.WorkflowConstants
import com.liferay.portal.model.Group
import com.liferay.portlet.journal.model.JournalArticle
import com.liferay.portal.service.GroupLocalServiceUtil
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil
import com.liferay.portal.security.auth.CompanyThreadLocal

import java.util.Comparator

class CleanArticleHistory {

    void cleanHistory() throws Exception {
        long countDeleted = 0
        int totalArticlesCount = JournalArticleLocalServiceUtil.getJournalArticlesCount()
        List<Group> toAdd = new ArrayList<Group>()
        toAdd.addAll(GroupLocalServiceUtil.getGroups(companyId, "com.liferay.portal.model.Company", 0))
        toAdd.addAll(GroupLocalServiceUtil.getGroups(companyId, "com.liferay.portal.model.Group", 0))

        for (Group curGroup : toAdd) {
            _log.info("PROCESSING GROUP: " + curGroup.getGroupId())
            DynamicQuery differentArticleIdsQuery = DynamicQueryFactoryUtil.forClass(JournalArticle.class, PortalClassLoaderUtil.getClassLoader())
            differentArticleIdsQuery.setProjection(ProjectionFactoryUtil.distinct(PropertyFactoryUtil.forName("articleId")))
            Criterion groupIdCriterion = RestrictionsFactoryUtil.eq("groupId", curGroup.getGroupId())
            differentArticleIdsQuery.add(groupIdCriterion)
            List<Object> differentIds = JournalArticleLocalServiceUtil.dynamicQuery(differentArticleIdsQuery)
            for (Object curId : differentIds) {
                _log.info("PROCESSING ARTICLEID: " + curId)
                JournalArticle lastVersion = JournalArticleLocalServiceUtil.fetchLatestArticle(curGroup.getGroupId(), (String) curId, WorkflowConstants.STATUS_APPROVED)
                if (lastVersion == null){
                    lastVersion=JournalArticleLocalServiceUtil.fetchLatestArticle(curGroup.getGroupId(), (String) curId, WorkflowConstants.STATUS_ANY)
                    _log.warn("THIS JOURNAL ARTICLE HAS NOT ANY APPROVED VERSION")
                    if(lastVersion==null) continue
                }

                double lastVersionApproved = lastVersion.getVersion()
                _log.info("MAX VERSION: " + lastVersionApproved)
                List<JournalArticle> allVersionArticles = ListUtil.sort(JournalArticleLocalServiceUtil.getArticles(curGroup.getGroupId(), (String) curId), versionComparator)
                int versionsCount = allVersionArticles.size()
                for (JournalArticle article : allVersionArticles) {
                    double version = article.getVersion()
                    if (versionsCount > leaveVersionCount && version < lastVersionApproved) {
                        if (deleteArticles) {
                            JournalArticleLocalServiceUtil.deleteArticle(article)
                        }
                        _log.debug("Deleted version: " + version + " of JournalArticle: " + curId)
                        versionsCount--; countDeleted++
                    }
                }
                _log.info("Current deleted: " + countDeleted + " from total: " + totalArticlesCount)
            }
        }
        _log.info("DELETED AT THE SCRIPT END: " + countDeleted + " FROM TOTAL: " + totalArticlesCount)
    }

    private static Log _log = LogFactoryUtil.getLog(CleanArticleHistory.class)
    private static boolean deleteArticles = true
    private static long companyId = CompanyThreadLocal.getCompanyId()
    private static long leaveVersionCount = 2
    private static Comparator<JournalArticle> versionComparator = new Comparator<JournalArticle>() {
        @Override
        int compare(JournalArticle o1, JournalArticle o2) {
            return Double.compare(o1.getVersion(), o2.getVersion())
        }

        @Override
        boolean equals(Object obj) {
            return this.equals(obj)
        }
    }
}

(new CleanArticleHistory()).cleanHistory()

