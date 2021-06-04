package br.com.zup

import com.google.rpc.Code
import com.google.protobuf.Any
import io.grpc.Status
import io.grpc.protobuf.StatusProto
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import javax.inject.Singleton
import kotlin.random.Random

@Singleton //Para ser reconhecida pelo micronaut
class FretesGrpcServer : FretesServiceGrpc.FretesServiceImplBase(){

    private val logger = LoggerFactory.getLogger(FretesGrpcServer::class.java)

    override fun calculaFrete(
        request: CalculaFreteRequest?,
        responseObserver: StreamObserver<CalculaFreteResponse>?
    ) {
        logger.info("Calculando frete para request: $request")

        val cep = request!!.cep

        if(cep == null || cep.isBlank()){
            responseObserver?.onError(Status.INVALID_ARGUMENT
                .withDescription("cep deve ser informado")
                .asRuntimeException())
            return
        }


        if(!request.cep.matches("[0-9]{5}-[\\d]{3}".toRegex())){
            responseObserver?.onError(Status.INVALID_ARGUMENT
                .withDescription("cep com formato inválido")
                .augmentDescription("O formato aceito é 99999-999")
                .asRuntimeException())
            return
        }


        val valor: Double
        try {
            valor = Random.nextDouble(from = 0.0, until = 140.0)
            if(valor > 100.0) {
                throw IllegalStateException("Erro inesperado ao executar logica de negócio!")
            }
        } catch (e: Exception){
            responseObserver?.onError(Status.INTERNAL
                .withDescription(e.message)
                .withCause(e) // anexado ao Status, mas não enviado ao Client
                .asRuntimeException())
        }

        //Simula um verificador de segurança
        if(cep.endsWith("333")){
            val statusProto = com.google.rpc.Status.newBuilder()
                .setCode(Code.PERMISSION_DENIED.number)
                .setMessage("usuário sem acesso ao recurso selecionado")
                .addDetails(Any.pack(
                    ErrorDetails.newBuilder()
                        .setCode(401)
                        .setMessage("Token expirado")
                        .build())
                )
                .build()

            responseObserver?.onError(
                StatusProto.toStatusRuntimeException(statusProto)
            )
        }

        val response = CalculaFreteResponse.newBuilder()
            .setCep(request!!.cep)
            .setValor(Random.nextDouble(from = 0.0, until = 140.0))
            .build()
        logger.info("Frete calculado para o cep ${response.cep}, no valor de R$ ${response.valor}")

        responseObserver!!.onNext(response)
        responseObserver.onCompleted()
    }
}