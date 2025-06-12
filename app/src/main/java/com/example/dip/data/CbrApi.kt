import com.example.dip.data.ValCurs
import retrofit2.http.GET
import retrofit2.Call

interface CbrApi {
    @GET("scripts/XML_daily.asp")
    fun getCurrencyRates(): Call<ValCurs>
}