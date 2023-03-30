package co.edu.icesi.drafts.service;


import co.edu.icesi.drafts.dto.IcesiUserDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface IcesiUserService {

    List<IcesiUserDTO> getAllUsers();

    IcesiUserDTO createUser(IcesiUserDTO userDTO);

    IcesiUserDTO getUser(UUID id);

}
