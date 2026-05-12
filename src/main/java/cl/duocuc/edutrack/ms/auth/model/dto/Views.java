package cl.duocuc.edutrack.ms.auth.model.dto;

public interface Views {

    interface Base {}

    interface Extra {}

    interface Detailed extends Base {}

    interface Create extends Base {}

    interface List extends Base, Extra {}

    interface Patch extends Base, Extra {}

    interface Update extends Base {}

    interface Admin extends Base, Extra {}

    interface Internal {}

    interface Login extends Base {}

    interface Refresh extends Base {}
}
