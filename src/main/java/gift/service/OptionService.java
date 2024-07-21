package gift.service;

import gift.dto.OptionRequestDTO;
import gift.dto.OptionResponseDTO;
import gift.entity.Option;
import gift.entity.Product;
import gift.exception.BadRequestExceptions.BadRequestException;
import gift.exception.BadRequestExceptions.InvalidIdException;
import gift.exception.BadRequestExceptions.NoSuchProductIdException;
import gift.exception.InternalServerExceptions.InternalServerException;
import gift.repository.OptionRepository;
import gift.repository.ProductRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OptionService {
    private final OptionRepository optionRepository;
    private final OptionRepositoryKeeperService optionRepositoryKeeperService;
    private final ProductRepository productRepository;

    public OptionService(OptionRepository optionRepository,
            OptionRepositoryKeeperService optionRepositoryKeeperService, ProductRepository productRepository) {
        this.optionRepository = optionRepository;
        this.optionRepositoryKeeperService = optionRepositoryKeeperService;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<OptionResponseDTO> getOneProductIdAllOptions(Long productId) {
        List<Option> optionList = optionRepository.findAllByProductId(productId);
        return optionList.stream().map(OptionResponseDTO::convertToDTO).toList();
    }

    @Transactional
    public void addOption(Long productId, OptionRequestDTO optionRequestDTO){
        try {
            Product product = productRepository.findById(productId).orElseThrow(
                    () -> new NoSuchProductIdException("해당 상품 Id인 상품을 찾지 못했습니다."));
            optionRepositoryKeeperService.checkUniqueOptionName(product, optionRequestDTO.name());
            optionRepository.save(optionRequestDTO.convertToOption(product));
        } catch (BadRequestException e) {
            throw e;
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("해당 상품에 이미 존재하는 옵션 이름 입니다.");
        } catch (Exception e) {
            throw new InvalidIdException(e.getMessage());
        }
    }

    @Transactional
    public void updateOption(Long productId, Long optionId, OptionRequestDTO optionRequestDTO){
        try {
            Product product = productRepository.findById(productId).orElseThrow(
                    () -> new NoSuchProductIdException("해당 상품 Id인 상품을 찾지 못했습니다."));
            Option optionToReplace = optionRequestDTO.convertToOption(product);
            Option optionInDb = optionRepository.findById(optionId).orElseThrow(
                    () -> new BadRequestException("그러한 Id를 가지는 옵션을 찾을 수 없습니다."));
            optionRepositoryKeeperService.checkUniqueOptionName(product, optionRequestDTO.name());
            optionInDb.changeOption(optionToReplace.getName(), optionToReplace.getQuantity());
        } catch (BadRequestException e) {
            throw e;
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException(e.getMessage());
        } catch (Exception e) {
            throw new InternalServerException(e.getMessage());
        }
    }

    @Transactional
    public void deleteOption(Long productId, Long optionId){
        try {
            Option optionToDelete = optionRepository.findByIdAndProductId(optionId, productId).orElseThrow(
                    () -> new BadRequestException("그러한 Id를 가지는 옵션을 찾을 수 없습니다."));
            if(optionRepository.countByProduct(optionToDelete.getProduct()) <= 1)
                throw new BadRequestException("상품에는 적어도 하나의 옵션이 있어야 해서 제거할 수 없습니다.");
            optionRepository.delete(optionToDelete);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalServerException(e.getMessage());
        }
    }

}
