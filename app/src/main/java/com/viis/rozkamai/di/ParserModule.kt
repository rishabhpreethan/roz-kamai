package com.viis.rozkamai.di

import com.viis.rozkamai.domain.parser.SmsParser
import com.viis.rozkamai.domain.parser.impl.AxisBankSmsParser
import com.viis.rozkamai.domain.parser.impl.FallbackSmsParser
import com.viis.rozkamai.domain.parser.impl.GPaySmsParser
import com.viis.rozkamai.domain.parser.impl.HdfcBankSmsParser
import com.viis.rozkamai.domain.parser.impl.IciciBankSmsParser
import com.viis.rozkamai.domain.parser.impl.PaytmSmsParser
import com.viis.rozkamai.domain.parser.impl.PhonePeSmsParser
import com.viis.rozkamai.domain.parser.impl.SbiBankSmsParser
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class ParserModule {

    @Binds @IntoSet abstract fun bindGPayParser(impl: GPaySmsParser): SmsParser
    @Binds @IntoSet abstract fun bindPhonePeParser(impl: PhonePeSmsParser): SmsParser
    @Binds @IntoSet abstract fun bindPaytmParser(impl: PaytmSmsParser): SmsParser
    @Binds @IntoSet abstract fun bindSbiParser(impl: SbiBankSmsParser): SmsParser
    @Binds @IntoSet abstract fun bindHdfcParser(impl: HdfcBankSmsParser): SmsParser
    @Binds @IntoSet abstract fun bindIciciParser(impl: IciciBankSmsParser): SmsParser
    @Binds @IntoSet abstract fun bindAxisParser(impl: AxisBankSmsParser): SmsParser
    @Binds @IntoSet abstract fun bindFallbackParser(impl: FallbackSmsParser): SmsParser
}
