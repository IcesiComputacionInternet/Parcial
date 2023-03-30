package co.edu.icesi.drafts.service.impl;

import co.edu.icesi.drafts.dto.IcesiDocumentDTO;
import co.edu.icesi.drafts.dto.IcesiUserDTO;
import co.edu.icesi.drafts.error.exception.DetailBuilder;
import co.edu.icesi.drafts.error.exception.ErrorCode;
import co.edu.icesi.drafts.mapper.IcesiUserMapper;
import co.edu.icesi.drafts.model.IcesiUser;
import co.edu.icesi.drafts.repository.IcesiUserRepository;
import co.edu.icesi.drafts.service.IcesiUserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static co.edu.icesi.drafts.error.util.IcesiExceptionBuilder.createIcesiException;

@AllArgsConstructor
public class IcesiUserServiceImpl implements IcesiUserService{

    private final IcesiUserRepository icesiUserRepository;

    private final IcesiUserMapper icesiUserMapper;

    public List<IcesiUserDTO> getAllUsers(){
        return icesiUserRepository.findAll().stream()
                .map(icesiUserMapper::fromIcesiUser)
                .toList();
    }

    @Override
    public IcesiUserDTO createUser(IcesiUserDTO userDTO){
        return icesiUserMapper.fromIcesiUser(icesiUserRepository.save(icesiUserMapper.fromIcesiUserDTO(userDTO)));
    }

    @Override
    public IcesiUserDTO getUser(UUID id){
        return icesiUserMapper.fromIcesiUser(icesiUserRepository.findById(id)
                .orElseThrow(
                        createIcesiException(
                                "User not found",
                                HttpStatus.NOT_FOUND,
                                new DetailBuilder(ErrorCode.ERR_404, "User", "Id", id)
                        )
                ));
    }
}
