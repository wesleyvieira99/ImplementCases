from dataclasses import dataclass
from typing import Protocol

import str


#Alerta usado
@dataclass(frozen=True)
class Alerta:
    evento_id: str
    conta_id: str
    push_habilitado: bool

class CanalIndisponivel(Exception):
    pass

#Canais usados nos envios dos alertas
class Canal(Protocol):
    def enviar(self, alerta: Alerta) -> None:
        ...

#Classe do roteador que envia alertas aos canais
class RoteadorAlertas:
    def __init__(self, push: Canal, sms: Canal) -> None:
        self._push = push
        self._sms = sms

    #Classe para processar o envio do alerta
    def processar(self, alerta: Alerta) -> str:
        if not isinstance(alerta.evento_id, str) or not alerta.evento_id.strip():
            raise ValueError("evento_id deve ser uma string nao vazia!")
        if not isinstance(alerta.conta_id, str) or not alerta.conta_id.strip():
            raise ValueError("conta_id deve ser uma string nao vazia!")

        if alerta.push_habilitado is True:
            canal = self._push
            resultado = "PUSH"

        else:
            canal = self._sms
            resultado = "SMS"

        canal.enviar(alerta)
        return resultado
