package com.deopraglabs.api_prysme.service;

import com.deopraglabs.api_prysme.controller.UserController;
import com.deopraglabs.api_prysme.data.model.Team;
import com.deopraglabs.api_prysme.data.model.User;
import com.deopraglabs.api_prysme.data.vo.UserVO;
import com.deopraglabs.api_prysme.mapper.custom.UserMapper;
import com.deopraglabs.api_prysme.repository.TeamRepository;
import com.deopraglabs.api_prysme.repository.UserRepository;
import com.deopraglabs.api_prysme.utils.DatabaseUtils;
import com.deopraglabs.api_prysme.utils.Utils;
import com.deopraglabs.api_prysme.utils.exception.CustomRuntimeException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Service
@Transactional
public class UserService implements UserDetailsService {

    private final Logger logger = Logger.getLogger(UserService.class.getName());

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    @Autowired
    public UserService(UserRepository userRepository, UserMapper userMapper, TeamRepository teamRepository) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.teamRepository = teamRepository;
    }

    public UserVO save(UserVO userVO) {
        logger.info("Saving user: " + userVO);
        final List<String> validations = validateUserInfo(userVO);

        if (!validations.isEmpty()) {
            throw new CustomRuntimeException.BRValidationException(validations);
        }

        if (userVO.getKey() > 0) {
            final var user = userRepository.findById(userVO.getKey())
                    .orElseThrow(() -> new CustomRuntimeException.UserNotFoundException(userVO.getKey()));
            return userMapper.convertToVO(
                    userRepository.save(userMapper.updateFromVO(user, userVO))).add(linkTo(methodOn(UserController.class).findById(userVO.getKey())).withSelfRel());
        } else {
            final var user = userRepository.save(userMapper.convertFromVO(userVO));

            if (user.getAuthorities().stream().anyMatch("MANAGER"::equals)) {
                final var team = teamRepository.save(new Team(0, user.getFullName(), user, new ArrayList<>()));
                user.setTeam(team);
            }
            return userMapper.convertToVO(userRepository.save(user)).add(linkTo(methodOn(UserController.class).findById(userVO.getKey())).withSelfRel());
        }
    }

    public List<UserVO> findAll() {
        logger.info("Finding all users");

        final var users = userMapper.convertToUserVOs(userRepository.findAll());
        users.forEach(user -> user.add(linkTo(methodOn(UserController.class).findById(user.getKey())).withSelfRel()));
        return users;
    }

    public List<UserVO> findAllByTeamId(long id) {
        logger.info("Finding all users by team id");
        final User auxUser = userRepository.findById(id).orElseThrow(() -> new CustomRuntimeException.UserNotFoundException(id));
        final var users = userMapper.convertToUserVOs(userRepository.findAllByTeamId(auxUser.getTeam().getId()));
        users.forEach(user -> user.add(linkTo(methodOn(UserController.class).findById(user.getKey())).withSelfRel()));
        return users;
    }

    public List<UserVO> findAllByManagerId(long id) {
        logger.info("Finding all users by manager id");
        final User auxUser = userRepository.findById(id).orElseThrow(() -> new CustomRuntimeException.UserNotFoundException(id));

        final List<User> userAux = new ArrayList<>();

        for (final User user : userRepository.findAll()) {
            if (user.getTeam().getId() == auxUser.getTeam().getId()) {
                userAux.add(user);
            }
        }

        final var users = userMapper.convertToUserVOs(userAux);
        users.forEach(user -> user.add(linkTo(methodOn(UserController.class).findById(user.getKey())).withSelfRel()));
        return users;
    }

    public UserVO findById(long id) {
        logger.info("Finding user by id: " + id);

        return userMapper.convertToVO(userRepository.findById(id)
                        .orElseThrow(() -> new CustomRuntimeException.UserNotFoundException(id)))
                .add(linkTo(methodOn(UserController.class).findById(id)).withSelfRel());
    }

    public ResponseEntity<?> delete(long id) {
        logger.info("Deleting user: " + id);

        if (userRepository.isDeleted(id) > 0) return ResponseEntity.notFound().build();
        return userRepository.softDeleteById(id, DatabaseUtils.generateRandomValue(id, 11)) > 0
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    public ResponseEntity<?> resetPassword(long id, String password) {
        var user = userRepository.findById(id).orElseThrow(() -> new CustomRuntimeException.UserNotFoundException(id));
        user.setPassword(Utils.encryptPassword(password));
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("Loading user by username: " + username);
        final var user = userRepository.findByUsername(username);

        if (user != null) {
            return user;
        } else {
            throw new UsernameNotFoundException(username);
        }
    }

    // Regras de Negócio
    private List<String> validateUserInfo(UserVO userVO) {
        final List<String> validations = new ArrayList<>();

        validateBasicFields(userVO, validations);
        validateUniqueFields(userVO, validations);

        return validations;
    }

    private void validateBasicFields(UserVO userVO, List<String> validations) {
        Utils.checkField(validations, Utils.isEmpty(userVO.getFirstName()), "First name is required");
        Utils.checkField(validations, Utils.isEmpty(userVO.getLastName()), "Last name is required");
        Utils.checkField(validations, Utils.isEmpty(userVO.getEmail()), "Email is required");
        Utils.checkField(validations, userVO.getBirthDate() == null, "Birth date is required");
        Utils.checkField(validations, userVO.getGender() == '\u0000', "Gender is required");
        Utils.checkField(validations, Utils.isEmpty(userVO.getPhoneNumber()), "Phone number is required");
//        Utils.checkField(validations, (Utils.isEmpty(userVO.getPassword()) && userVO.getKey() < 1), "Password is required");
    }

    private void validateUniqueFields(UserVO userVO, List<String> validations) {
        if (!Utils.isEmpty(userVO.getEmail())
                && userRepository.findByEmailAndIdNot(userVO.getEmail(), userVO.getKey()) != null) {
            validations.add("Email is already associated with another account");
        }
        if (!Utils.isEmpty(userVO.getPhoneNumber())
                && userRepository.findByPhoneNumberAndIdNot(userVO.getPhoneNumber(), userVO.getKey()) != null) {
            validations.add("Phone number is already associated with another account");
        }
    }
}
