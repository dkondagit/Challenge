package challenge.webside.controllers.util;

import challenge.dbside.models.Image;
import challenge.dbside.models.User;
import challenge.dbside.services.ini.MediaService;
import challenge.webside.authorization.UserActionsProvider;
import challenge.webside.authorization.thymeleaf.AuthorizationDialect;
import challenge.webside.dao.UsersDao;
import challenge.webside.imagesstorage.ImageStoreService;
import challenge.webside.model.UserProfile;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
public class UserUtil {

    @Autowired
    @Qualifier("storageServiceUser")
    private MediaService serviceEntity;

    @Autowired
    private UserActionsProvider actionsProvider;

    @Autowired
    private AuthorizationDialect dialect;

    @Autowired
    private UsersDao usersDao;

    public void setProfileShow(int userDBId, HttpServletRequest request, UserProfile userProfile, User user, Model model) {
        User userWhichProfileRequested = (User) serviceEntity.findById(userDBId, User.class);
        User signedUpUser = (User) serviceEntity.findById(userProfile.getUserEntityId(), User.class);

        model.addAttribute("userProfile", userWhichProfileRequested);
        model.addAttribute("listOfDefined", userWhichProfileRequested.getChallenges());
        model.addAttribute("currentDBUser", user);
        model.addAttribute("listOfAccepted", userWhichProfileRequested.getAcceptedChallenges());
        model.addAttribute("listOfSubscripted", userWhichProfileRequested.getSubscriptions());
        dialect.setActions(actionsProvider.getActionsForProfile(signedUpUser, userWhichProfileRequested));
        model.addAttribute("friends", signedUpUser.getFriends());
        model.addAttribute("mapOfNetworks", usersDao.getListOfNetworks(userDBId));
    }
    
    public List<User> filterUsers(String filter) {
        List<User> allUsers = serviceEntity.getAll(User.class);
        List<User> filteredUsers = new ArrayList<>();
        for (User user : allUsers) {
            String name = user.getName();
            if (name.toLowerCase().startsWith(filter.toLowerCase())) {
                filteredUsers.add(user);
            }
        }
        return filteredUsers;
    }

    public void setModelForShowNotFriends(HttpServletRequest request, User currentUser, Model model) {
        List<User> allUsers = serviceEntity.getAll(User.class);
        List<User> friends = currentUser.getFriends();
        List<User> filteredUsers = new ArrayList<>();
        for (User user : allUsers) {
            if (!friends.contains(user)) {
                filteredUsers.add(user);
            }
        }
        model.addAttribute("users", filteredUsers);
        model.addAttribute("curUser", currentUser);
        model.addAttribute("showingAllUsers", false);
    }

    public void setModelForShowFriends(HttpServletRequest request, Principal currentUser, Model model, int userId) {
        User user = (User) serviceEntity.findById(userId, User.class);
        model.addAttribute("listSomething", user.getFriends());
        model.addAttribute("idParent", userId);
        model.addAttribute("handler", "profile");

    }

    public List<User> filterFriends(String filter, int userId) {
        User user = (User) serviceEntity.findById(userId, User.class);

        List<User> allFriends = user.getFriends();
        List<User> filteredFriends = new ArrayList<>();
        for (User friend : allFriends) {
            String name = friend.getName();
            if (name.toLowerCase().startsWith(filter.toLowerCase())) {
                filteredFriends.add(friend);
            }
        }
        return filteredFriends;
    }

    public void setModelForUpdatedProfile(User user, HttpServletRequest request, Principal currentUser, Model model, String image) {
        User userToUpdate = (User) serviceEntity.findById(user.getId(), User.class);
        userToUpdate.setName(user.getName());
        //TODO:check if creator
        serviceEntity.update(userToUpdate);

        user = (User) serviceEntity.findById(user.getId(), User.class);

        if (!image.isEmpty() && !StringUtils.isNumeric(image)) {
            String base64Image = image.split(",")[1];
            byte[] array = Base64.decodeBase64(base64Image);
            Image imageEntity = new Image();
            imageEntity.setIsMain(Boolean.TRUE);
            serviceEntity.save(imageEntity);

            Image oldImage = user.getMainImageEntity();
            oldImage.setIsMain(Boolean.FALSE);
            serviceEntity.update(oldImage);

            try {
                ImageStoreService.saveImage(array, imageEntity);
                serviceEntity.update(imageEntity);
            } catch (Exception ex) {
                Logger.getLogger(UsersDao.class.getName()).log(Level.SEVERE, null, ex);
            }
            user.addImage(imageEntity);
            serviceEntity.update(user);
        } else if (StringUtils.isNumeric(image)) {
            Image oldImage = user.getMainImageEntity();
            oldImage.setIsMain(Boolean.FALSE);
            serviceEntity.update(oldImage);

            Image newMainImage = (Image) serviceEntity.findById(Integer.valueOf(image), Image.class);
            newMainImage.setIsMain(Boolean.TRUE);
            serviceEntity.update(newMainImage);
        }
    }

    public void setModelForUsers(HttpServletRequest request, User currentUser, Model model) {
        List<User> users = serviceEntity.getAll(User.class);
        Collections.sort(users, User.COMPARE_BY_RATING);
        model.addAttribute("curUser", currentUser);
        model.addAttribute("users", users);
        model.addAttribute("showingAllUsers", true);
    }

    public void setModelForAddFriend(HttpServletRequest request, User user, Model model, int friendId) {
        User friend = (User) serviceEntity.findById(friendId, User.class);
        friend.addFriend(user);
        serviceEntity.update(friend);
        user.addFriend(friend);
        serviceEntity.update(user);
    }

    public void setModelForEditProfile(int userId, HttpServletRequest request, Principal currentUser, Model model) {
        User user = (User) serviceEntity.findById(userId, User.class);
        model.addAttribute("userProfile", user);
        model.addAttribute("mapOfNetworks", usersDao.getListOfNetworks(userId));
    }
}
