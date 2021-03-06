package challenge.webside.controllers.util;

import challenge.dbside.models.ChallengeDefinition;
import challenge.dbside.models.ChallengeInstance;
import challenge.dbside.models.ChallengeStep;
import challenge.dbside.models.Image;
import challenge.dbside.models.User;
import challenge.dbside.models.dbentity.DBSource;
import challenge.dbside.models.status.ChallengeStatus;
import challenge.dbside.services.ini.MediaService;
import challenge.webside.authorization.UserActionsProvider;
import challenge.webside.authorization.thymeleaf.AuthorizationDialect;
import challenge.webside.dao.UsersDao;
import challenge.webside.imagesstorage.ImageStoreService;
import java.security.Principal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
public class ChallengeInstanceUtil {

    @Autowired
    @Qualifier("storageServiceUser")
    private MediaService serviceEntity;

    @Autowired
    private AuthorizationDialect dialect;

    @Autowired
    private UserActionsProvider actionsProvider;

    @Autowired
    private CommentUtil commentUtil;

    public void setModelForNewStepForChallenge(HttpServletRequest request, Principal currentUser, Model model, ChallengeStep step, String image, int chalId) {
        ChallengeInstance currentChallenge = (ChallengeInstance) serviceEntity.findById(chalId, ChallengeInstance.class);
        serviceEntity.save(step);
        //need to update or create image
        if (!image.isEmpty() && !StringUtils.isNumeric(image)) {
            String base64Image = image.split(",")[1];
            byte[] array = Base64.decodeBase64(base64Image);
            Image imageEntity = new Image();
            imageEntity.setIsMain(Boolean.TRUE);
            serviceEntity.save(imageEntity);
            try {
                ImageStoreService.saveImage(array, imageEntity);
                serviceEntity.update(imageEntity);
            } catch (Exception ex) {
                Logger.getLogger(UsersDao.class.getName()).log(Level.SEVERE, null, ex);
            }
            step.addImage(imageEntity);
            serviceEntity.update(step);
        } else if (StringUtils.isNumeric(image)) {
            Image newMainImage = (Image) serviceEntity.findById(Integer.valueOf(image), Image.class);
            newMainImage.setIsMain(Boolean.TRUE);
            serviceEntity.update(newMainImage);
        }

        currentChallenge.addStep(step);
        serviceEntity.update(currentChallenge);
    }

    public void setModelForChallengeInstanceShow(int id, HttpServletRequest request, User user, Model model) {

        ChallengeInstance challenge = (ChallengeInstance) serviceEntity.findById(id, ChallengeInstance.class);
        checkAndUpdateIfOutdated(challenge);
        dialect.setActions(actionsProvider.getActionsForChallengeInstance(user, challenge));
        model.addAttribute("challenge", challenge);
        ChallengeStep step = new ChallengeStep();
        step.setDate(new Date());
        model.addAttribute("step", step);
        model.addAttribute("showStepForm", false);
        model.addAttribute("dateError", false);
        model.addAttribute("userProfile", user);
        List<ChallengeStep> listOfSteps = challenge.getSteps();
        Collections.sort(listOfSteps, ChallengeStep.COMPARE_BY_DATE);
        model.addAttribute("listOfSteps", listOfSteps);
        commentUtil.setModelForComments(challenge.getComments(), request, user, model);
    }

    public void setModelForInstanceSubscribe(HttpServletRequest request, User user, Model model, int chalId) {
        ChallengeInstance challenge = (ChallengeInstance) serviceEntity.findById(chalId, ChallengeInstance.class);
        user.addSubscription(challenge);
        serviceEntity.update(user);
        challenge.addSubscriber(user);
        serviceEntity.update(challenge);
    }

    public void setModelForCloseChallenge(HttpServletRequest request, Principal currentUser, Model model, int chalId) {
        ChallengeInstance challengeToClose = (ChallengeInstance) serviceEntity.findById(chalId, ChallengeInstance.class);
        challengeToClose.setStatus(ChallengeStatus.PUT_TO_VOTE);
        challengeToClose.setClosingDate(new Date());
        serviceEntity.update(challengeToClose);
    }

    public void setModelForAcceptOrDeclineChallenge(HttpServletRequest request, User user, Model model, int chalId, boolean accept) {
        ChallengeInstance chalToAccept = (ChallengeInstance) serviceEntity.findById(chalId, ChallengeInstance.class);

        if (accept) {
            user.acceptChallenge(chalToAccept);
        } else {
            for (DBSource childDB : chalToAccept.getDataSource().getChildren()) {
                serviceEntity.delete(new Image(childDB));
            }
            serviceEntity.delete(chalToAccept);
        }
        serviceEntity.update(user);
        dialect.setActions(actionsProvider.getActionsForProfile(user, user));
    }

    public void setModelForBadStepChal(int chalId, ChallengeStep step, HttpServletRequest request, User user, Model model, String image, String imageName) {

        ChallengeInstance challenge = (ChallengeInstance) serviceEntity.findById(chalId, ChallengeInstance.class);
        checkAndUpdateIfOutdated(challenge);
        dialect.setActions(actionsProvider.getActionsForChallengeInstance(user, challenge));
        model.addAttribute("challenge", challenge);
        model.addAttribute("userProfile", user);
        List<ChallengeStep> listOfSteps = challenge.getSteps();
        Collections.sort(listOfSteps, ChallengeStep.COMPARE_BY_DATE);
        model.addAttribute("listOfSteps", listOfSteps);
        commentUtil.setModelForComments(challenge.getComments(), request, user, model);
        model.addAttribute("step", step);
        model.addAttribute("showStepForm", true);
        model.addAttribute("image64", image);
        model.addAttribute("imageName", imageName);
    }

    public void checkAndUpdateIfOutdated(ChallengeInstance challenge) {
        Date closingDate = challenge.getClosingDate();
        Date currentDate = new Date();
        long diffInMillies = currentDate.getTime() - closingDate.getTime();
        long diff = TimeUnit.MINUTES.convert(diffInMillies, TimeUnit.MILLISECONDS);
        synchronized (challenge) {
            if (diff >= 5 && challenge.getStatus() == ChallengeStatus.PUT_TO_VOTE) {
                int votesFor = challenge.getVotesFor().size();
                int votesAgainst = challenge.getVotesAgainst().size();
                challenge.setStatus(votesFor > votesAgainst ? ChallengeStatus.COMPLETED : ChallengeStatus.FAILED);
                serviceEntity.update(challenge);
                User authorUser = challenge.getAcceptor();
                authorUser.addRating(votesFor - votesAgainst);
                serviceEntity.update(authorUser);
                ChallengeDefinition challengeDef = challenge.getChallengeRoot();
                challengeDef.addRating(votesFor - votesAgainst);
                serviceEntity.update(challengeDef);
            } else if (currentDate.compareTo(challenge.getDate()) >= 0 && challenge.getStatus() == ChallengeStatus.ACCEPTED) {
                challenge.setStatus(ChallengeStatus.PUT_TO_VOTE);
                challenge.setClosingDate(new Date());
                serviceEntity.update(challenge);
            }
        }
    }

}
