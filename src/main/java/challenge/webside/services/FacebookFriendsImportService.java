package challenge.webside.services;

import challenge.dbside.models.User;
import challenge.dbside.services.ini.MediaService;
import challenge.webside.model.UserConnection;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.api.PagedList;
import org.springframework.social.facebook.api.Reference;
import org.springframework.social.facebook.api.impl.FacebookTemplate;
import org.springframework.stereotype.Service;

@Service("facebookFriendsService")
public class FacebookFriendsImportService implements FriendsImportService {

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    @Qualifier("storageServiceUser")
    private MediaService serviceEntity;

    private List<User> friends;

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public FacebookFriendsImportService(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public List<User> importFriends(UserConnection connection) {
        if (friends == null) {
            Connection<Facebook> conn = connectionRepository.findPrimaryConnection(Facebook.class);
            Facebook facebook = conn != null
                    ? conn.getApi()
                    : new FacebookTemplate(connection.getAccessToken());
            PagedList<Reference> facebookFriends = facebook.friendOperations().getFriends();
            friends = new ArrayList<>();
            for (Reference profile : facebookFriends) {
                Integer id;
                try {
                    id = jdbcTemplate.queryForObject("SELECT userentityid FROM userprofile p "
                            + "JOIN userconnection c ON p.userid = c.userid WHERE c.providerid = ? "
                            + "AND c.provideruserid = ?",
                            new Object[]{connection.getProviderId(), profile.getId()}, Integer.class);
                } catch (EmptyResultDataAccessException e) {
                    id = null;
                }
                if (id != null) {
                    User user = (User) serviceEntity.findById(id, User.class);
                    friends.add(user);
                }
            }
        }
        return friends;
    }
}
