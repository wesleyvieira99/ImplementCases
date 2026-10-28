from unittest.mock import Mock

from alertas import RoteadorAlertas, Alerta


def test_envia_push_sem_acionar_sms():
    push = Mock(spec=["enviar"])
    sms = Mock(spec=["enviar"])
    roteador = RoteadorAlertas(push=push, sms=sms)
    alerta = Alerta(
        evento_id="evt-1",
        conta_id = "conta-1",
        push_habilitado = True
    )

    resultado = roteador.processar(alerta)

    assert resultado == "PUSH"
    push.enviar.assert_called_once_with(alerta)
    sms.enviar.assert_not_called()

def test_propaga_falha_sem_retry_ou_fallback():
    push = Mock(spec=["enviar"])
    sms = Mock(spec=["enviar"])
    falha = CanalIndisponivel("push indisponivel")
    push.enviar.side_effect = falha
    roteador = RoteadorAlertas(push=push, sms=sms)
    alerta = Alerta(
        evento_id="evt-2",
        conta_id="conta-1",
        push_habilitado=True,
    )

    with pytest.raises(CanalIndisponivel) as erro:
        roteador.processar(alerta)

    assert erro.value is falha
    push.enviar.assert_called_once_with(alerta)
    sms.enviar.assert_not_called()

@pytest.mark.parametrize("campo", ["evento_id", "conta_id"])
@pytest.mark.parametrize("valor", ["", "   ", "\t\n", None, 123])
def test_rejeita_id_invalido_sem_enviar(campo, valor):
    push = Mock(spec=["enviar"])
    sms = Mock(spec=["enviar"])
    roteador = RoteadorAlertas(push=push, sms=sms)

    dados = {"evento_id": "evt-4", "conta_id": "conta-1", "push_habilitado": True, campo: valor}
    alerta = Alerta(**dados)

    with pytest.raises(ValueError):
        roteador.processar(alerta)

    push.enviar.assert_not_called()
    sms.enviar.assert_not_called()