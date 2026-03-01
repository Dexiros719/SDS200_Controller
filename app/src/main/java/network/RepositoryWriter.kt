package network

/*
Repository writer – Writes translated data to repository
*/
class RepositoryWriter(private val repository: ScannerRepository) {

    /*
     Writes data
    */
    fun writeStatus(status: ScannerStatus) {
        repository.updateStatus(status)
    }

}